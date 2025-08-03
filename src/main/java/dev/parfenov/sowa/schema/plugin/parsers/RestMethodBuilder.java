package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.git.DependencySearcher;
import dev.parfenov.sowa.schema.plugin.parsers.dto.Entity;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;
import io.github.classgraph.MethodInfo;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;

/**
 * Builder для создания объектов RestClassMethod.
 * <p>
 * Предоставляет удобный способ пошагового конструирования объектов
 * REST методов с возможностью условной установки значений.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class RestMethodBuilder {
    private final MethodInfo method;
    private final Method rawMethod;
    private final MethodModel methodModel;
    private final EndpointPathParser endpointPathParser;
    private final boolean onlyGitDiff;
    private final DependencySearcher dependencySearcher;

    public RestMethodBuilder(final MethodInfo methodInfo,
                             final MethodModel existing,
                             final EndpointPathParser endpointPathParser,
                             final boolean onlyGitDiff,
                             final DependencySearcher dependencySearcher) {
        this.method = methodInfo;
        this.rawMethod = methodInfo.loadClassAndGetMethod();
        this.methodModel = Optional.ofNullable(existing).orElseGet(MethodModel::new);
        this.endpointPathParser = endpointPathParser;
        this.onlyGitDiff = onlyGitDiff;
        this.dependencySearcher = dependencySearcher;
    }

    /**
     * Устанавливает имя метода.
     *
     * @param uniqueMethodName уникальное имя метода
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withName(String uniqueMethodName) {
        methodModel.setName(uniqueMethodName);
        return this;
    }

    /**
     * Устанавливает переменные пути, если они не заданы.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withPathVariables() {
        if (CollectionUtils.isEmpty(methodModel.getPathVariables())) {
            var pathVariables = MethodExtractor.extractPathVariables(rawMethod);
            methodModel.setPathVariables(pathVariables);
        }
        return this;
    }

    /**
     * Устанавливает тип запроса, если он не задан.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withRequest() {
        methodModel.setRequest(withEntity(MethodExtractor.extractRequest(rawMethod)));
        return this;
    }

    /**
     * Устанавливает тип ответа, если он не задан.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withResponse() {
        methodModel.setResponse(withEntity(MethodExtractor.extractResponse(rawMethod)));
        return this;
    }

    /**
     * Создает {@link Entity} и устанавливает тип
     *
     * @param type тип
     * @return {@link Entity}
     */
    private Entity withEntity(Type type) {
        var entity = new Entity();
        entity.setType(type);
        return entity;
    }

    /**
     * Устанавливает путь эндпоинта, если он не задан.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withEndpointPath() {
        if (!StringUtils.hasText(methodModel.getEndpointPath())) {
            var endpointPath = endpointPathParser.getEndpointPath(method);
            methodModel.setEndpointPath(endpointPath);
        }
        return this;
    }

    /**
     * Устанавливает HTTP метод, если он не задан.
     *
     * @return builder для цепочки вызовов
     */
    public RestMethodBuilder withHttpMethod() {
        if (methodModel.getHttpMethod() == null) {
            AnnotationParser
                    .getHttpMethod(method)
                    .ifPresent(methodModel::setHttpMethod);
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
            var dependencies = dependencySearcher.searchDependencies(method);
            setDependencies(methodModel.getRequest(), dependencies.getRequest());
            setDependencies(methodModel.getResponse(), dependencies.getResponse());
        }
        return this;
    }

    private void setDependencies(Entity entity, Set<String> dependencies) {
        entity.setDependencies(dependencies);
    }

    /**
     * Устанавливает {@link Void} для нулевого {@link Entity#getType()}
     * и возвращает построенный объект REST метода.
     *
     * @return готовый объект RestClassMethod
     */
    public MethodModel build() {
        nullableTypeToVoid(methodModel.getRequest());
        return methodModel;
    }

    /**
     * Установка {@link Void} для нулевого {@link Entity#getType()}
     */
    private void nullableTypeToVoid(Entity entity) {
        if (entity.getType() == null) {
            entity.setType(Void.class);
        }
    }
} 