package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.config.ClassParserConfig;
import dev.parfenov.sowa.schema.plugin.git.DependencySearcher;
import dev.parfenov.sowa.schema.plugin.git.GitDiffParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Парсер Spring REST контроллеров.
 * <p>
 * Анализирует классы с аннотациями @RestController и @Controller,
 * извлекает информацию о методах, путях эндпоинтов, параметрах запросов
 * и типах ответов для дальнейшей генерации схем.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class ClassParser {

    private final EndpointPathParser endpointPathParser;
    private final ClassParserConfig config;
    private final GitDiffParser gitDiffParser;
    private final String[] projectBasePackages;

    /**
     * Создает парсер классов с указанной конфигурацией.
     *
     * @param classParserConfig конфигурация парсера
     */
    public ClassParser(final ClassParserConfig classParserConfig) {
        this.endpointPathParser = new EndpointPathParser(classParserConfig.contextPath());
        this.config = classParserConfig;
        this.gitDiffParser = new GitDiffParser(classParserConfig.branchDiffWith(), classParserConfig.onlyGitDiff());
        this.projectBasePackages = classParserConfig.projectBasePackages();
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
    @SuppressWarnings({"unchecked"})
    public List<ClassModel> parseAllRestClasses(ScanResult scanResult) {
        var restControllers = scanResult
                .getClassesWithAnyAnnotation(RestController.class, Controller.class)
                .filter(this::isProjectPackage)
                .stream()
                .toList();

        var dependencySearcher = new DependencySearcher(scanResult, this);
        return restControllers.stream()
                .map(classInfo -> parseRestController(classInfo, dependencySearcher))
                .peek(gitDiffParser::diff)
                .toList();
    }

    /**
     * Проверяет, принадлежит ли класс к пакету проекта.
     *
     * @param classInfo информация о классе
     * @return true если класс в пакете проекта
     */
    public boolean isProjectPackage(ClassInfo classInfo) {
        return Arrays
                .stream(projectBasePackages)
                .anyMatch(packageName ->
                        classInfo.getPackageName().startsWith(packageName)
                );
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
    public ClassModel parseRestController(ClassInfo restController, DependencySearcher dependencySearcher) {
        var builder = new RestClassBuilder(endpointPathParser, config.onlyGitDiff(), dependencySearcher)
                .withName(restController.getSimpleName())
                .withMainClass(restController);

        for (var interfaceInfo : restController.getInterfaces()) {
            builder.withInterface(interfaceInfo);
        }

        return builder.build();
    }
}
