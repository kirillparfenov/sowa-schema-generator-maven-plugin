/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.classloader.ClassLoader;
import dev.parfenov.sowa.schema.plugin.git.GitDiffParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import io.github.classgraph.ClassInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Парсер Spring REST контроллеров.
 * <p>
 * Анализирует классы с аннотациями @RestController и @Controller,
 * извлекает информацию о методах, путях эндпоинтов, параметрах запросов
 * и типах ответов для дальнейшей генерации схем.
 */
public class ClassParser {

    private final ClassLoader classLoader;
    private final EndpointPathParser endpointPathParser;
    private final TypesParser typesParser = new TypesParser();
    private final ClassParserConfig config;

    /**
     * Создает парсер классов с указанной конфигурацией.
     *
     * @param classParserConfig конфигурация парсера
     */
    public ClassParser(final ClassParserConfig classParserConfig) {
        this.classLoader = new ClassLoader(classParserConfig);
        this.endpointPathParser = new EndpointPathParser(classParserConfig.project());
        this.config = classParserConfig;
    }

    /**
     * Парсит все REST контроллеры в проекте.
     * <p>
     * Сканирует classpath на наличие классов с аннотациями @RestController
     * или @Controller, фильтрует по пакету проекта и при необходимости
     * применяет фильтрацию по git diff.
     *
     * @return список проанализированных REST классов
     */
    public List<RestClass> parseAllRestClasses() {
        try (var scanResult = classLoader.getClassgraph().scan()) {
            var result = scanResult
                    .getClassesWithAnyAnnotation(RestController.class, Controller.class)
                    .filter(this::isProjectPackage)
                    .stream()
                    .map(this::parseRestController)
                    .toList();

            if (config.onlyGitDiff()) {
                new GitDiffParser(config.branchDiffWith(), scanResult, config.projectBasePackage()).setNullForNoDiff(result);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время сканирования графа классов", e);
        }
    }

    /**
     * Проверяет, принадлежит ли класс к пакету проекта.
     *
     * @param controllerClass информация о классе контроллера
     * @return true если класс в пакете проекта
     */
    private boolean isProjectPackage(ClassInfo controllerClass) {
        return controllerClass
                .getPackageInfo()
                .getName()
                .startsWith(classLoader.baseProjectPackage());
    }

    /**
     * Парсит отдельный REST контроллер.
     * <p>
     * Извлекает информацию о классе и его методах, включая
     * интерфейсы которые он реализует.
     *
     * @param restController информация о классе контроллера
     * @return объект REST класса с методами
     */
    public RestClass parseRestController(ClassInfo restController) {
        var builder = new RestClassBuilder(endpointPathParser, typesParser)
                .withName(restController.getSimpleName())
                .withMainClass(restController);

        for (var interfaceInfo : restController.getInterfaces()) {
            builder.withInterface(interfaceInfo);
        }

        return builder.build();
    }
}
