/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.git.GraphBuilder;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builder для создания объектов RestClass.
 * <p>
 * Предоставляет удобный способ пошагового конструирования объектов
 * REST классов с обработкой основного класса и интерфейсов.
 */
public class RestClassBuilder {

    private final RestClass restClass;
    private final List<RestMethod> restMethods = new ArrayList<>();
    private final Set<String> restMethodNames = new HashSet<>();
    private final EndpointPathParser endpointPathParser;
    private final boolean onlyGitDiff;
    private final GraphBuilder graphBuilder;

    /**
     * Конструктор builder'а.
     *
     * @param endpointPathParser парсер путей эндпоинтов
     * @param typesParser        парсер типов
     */
    public RestClassBuilder(final EndpointPathParser endpointPathParser,
                            final boolean onlyGitDiff,
                            final GraphBuilder graphBuilder) {
        this.restClass = new RestClass();
        this.endpointPathParser = endpointPathParser;
        this.onlyGitDiff = onlyGitDiff;
        this.graphBuilder = graphBuilder;
    }

    /**
     * Устанавливает имя класса.
     *
     * @param name имя класса
     * @return builder для цепочки вызовов
     */
    public RestClassBuilder withName(String name) {
        restClass.setName(name);
        return this;
    }

    /**
     * Обрабатывает основной класс контроллера.
     *
     * @param classInfo информация о классе
     * @return builder для цепочки вызовов
     */
    public RestClassBuilder withMainClass(ClassInfo classInfo) {
        processClass(classInfo);
        return this;
    }

    /**
     * Добавляет интерфейс к обработке.
     *
     * @param interfaceInfo информация об интерфейсе
     * @return builder для цепочки вызовов
     */
    public RestClassBuilder withInterface(ClassInfo interfaceInfo) {
        processClass(interfaceInfo);
        return this;
    }

    /**
     * Возвращает построенный объект REST класса.
     *
     * @return готовый объект RestClass
     */
    public RestClass build() {
        restClass.setMethods(restMethods);
        return restClass;
    }

    /**
     * Обрабатывает класс или интерфейс, извлекая методы и устанавливая путь.
     *
     * @param classInfo информация о классе/интерфейсе
     */
    private void processClass(ClassInfo classInfo) {
        setEndpointPathIfEmpty(classInfo);
        extractClassMethods(classInfo);
    }

    /**
     * Устанавливает путь эндпоинта, если он не задан.
     *
     * @param classInfo информация о классе
     */
    private void setEndpointPathIfEmpty(ClassInfo classInfo) {
        if (!StringUtils.hasText(restClass.getEndpointPath())) {
            var endpointPath = endpointPathParser.pathOnClass(classInfo);
            restClass.setEndpointPath(endpointPath);
        }
    }

    /**
     * Извлекает методы класса с аннотациями маппинга.
     *
     * @param classInfo информация о классе
     */
    private void extractClassMethods(ClassInfo classInfo) {
        //todo нужно уйти от typeResolver и работать с ClassInfo

        for (var method : classInfo.getMethodInfo()) {
            var requestMapping = method.getAnnotationInfo(RequestMapping.class);
            if (requestMapping != null) {
                var restMethod = buildRestMethod(method, new RestMethod());
                restMethods.add(restMethod);
                restMethodNames.add(restMethod.getName());
            }
        }
    }

    /**
     * Строит объект REST метода из Java метода.
     *
     * @param methodInfo  метод classGraph {@link MethodInfo}
     * @param builtMethod метод мы строим
     * @return построенный REST метод
     */
    private RestMethod buildRestMethod(MethodInfo methodInfo, RestMethod builtMethod) {
        return new RestMethodBuilder(methodInfo, builtMethod, endpointPathParser, onlyGitDiff, graphBuilder)
                .withName(createUniqueMethodName(methodInfo))
                .withPathVariables()
                .withRequest()
                .withResponse()
                .withEndpointPath()
                .withHttpMethod()
                .withDependencies()
                .build();
    }

    /**
     * Создает уникальное имя метода с MD5 хешем.
     *
     * @param methodInfo метод для обработки
     * @return уникальное имя метода
     */
    private String createUniqueMethodName(MethodInfo methodInfo) {
        var methodName = methodInfo.getName();
        if (!restMethodNames.contains(methodName)) return methodName;

        var bytes = methodInfo.toString().getBytes(StandardCharsets.UTF_8);
        var hash = DigestUtils.md5DigestAsHex(bytes).substring(0, 4);
        return methodName + "_" + hash;
    }
} 