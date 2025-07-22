/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.git.GraphBuilder;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;
import io.github.classgraph.MethodInfo;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Builder для создания объектов RestClassMethod.
 * <p>
 * Предоставляет удобный способ пошагового конструирования объектов
 * REST методов с возможностью условной установки значений.
 */
public class RestMethodBuilder {
    private final MethodInfo method;
    private final Method rawMethod; //todo посмотреть - мб полностью заменить rawMethod на method
    private final RestMethod restMethod;
    private final EndpointPathParser endpointPathParser;
    private final boolean onlyGitDiff;
    private final GraphBuilder graphBuilder;

    /**
     * Конструктор builder'а.
     *
     * @param methodInfo         Java метод для анализа
     * @param existing           существующий объект RestClassMethod или null
     * @param endpointPathParser парсер путей эндпоинтов
     */
    public RestMethodBuilder(final MethodInfo methodInfo,
                             final RestMethod existing,
                             final EndpointPathParser endpointPathParser,
                             final boolean onlyGitDiff,
                             final GraphBuilder graphBuilder) {
        this.method = methodInfo;
        this.rawMethod = methodInfo.loadClassAndGetMethod();
        this.restMethod = Optional.ofNullable(existing).orElseGet(RestMethod::new);
        this.endpointPathParser = endpointPathParser;
        this.onlyGitDiff = onlyGitDiff;
        this.graphBuilder = graphBuilder;
    }

    /**
     * Устанавливает имя метода.
     *
     * @param uniqueMethodName уникальное имя метода
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withName(String uniqueMethodName) {
        restMethod.setName(uniqueMethodName);
        return this;
    }

    /**
     * Устанавливает переменные пути, если они не заданы.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withPathVariables() {
        if (CollectionUtils.isEmpty(restMethod.getPathVariables())) {
            var pathVariables = MethodExtractor.extractPathVariables(rawMethod);
            restMethod.setPathVariables(pathVariables);
        }
        return this;
    }

    /**
     * Устанавливает тип запроса, если он не задан.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withRequest() {
        if (restMethod.getRequest() == null) {
            restMethod.setRequest(MethodExtractor.extractRequest(rawMethod));
        }
        return this;
    }

    /**
     * Устанавливает тип ответа, если он не задан.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withResponse() {
        if (restMethod.getResponse() == null) {
            restMethod.setResponse(MethodExtractor.extractResponse(rawMethod));
        }
        return this;
    }

    /**
     * Устанавливает путь эндпоинта, если он не задан.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withEndpointPath() {
        if (!StringUtils.hasText(restMethod.getEndpointPath())) {
            var endpointPath = endpointPathParser.pathOnMethod(rawMethod);
            restMethod.setEndpointPath(endpointPath);
        }
        return this;
    }

    /**
     * Устанавливает HTTP метод, если он не задан.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withHttpMethod() {
        if (restMethod.getHttpMethod() == null) {
            var requestMapping = AnnotationParser.directOnMethodOrAnnotations(RequestMapping.class, rawMethod);
            if (requestMapping != null && requestMapping.method().length > 0) {
                restMethod.setHttpMethod(requestMapping.method()[0].asHttpMethod());
            }
        }
        return this;
    }

    /**
     * Строит зависимости source-files, если включена опция onlyGitDiff
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withDependencies() {
        if (onlyGitDiff) {
            var dependencies = graphBuilder.buildGraphModels(method);
            restMethod.setDependencies(dependencies);
        }
        return this;
    }

    /**
     * Возвращает построенный объект REST метода.
     *
     * @return готовый объект RestClassMethod
     */
    public RestMethod build() {
        return restMethod;
    }
} 