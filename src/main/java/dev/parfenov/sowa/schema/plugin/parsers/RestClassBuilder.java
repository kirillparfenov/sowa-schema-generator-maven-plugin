/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.git.DependencySearcher;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;
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

    private final ClassModel classModel;
    private final List<MethodModel> methodModels = new ArrayList<>();
    private final Set<String> restMethodNames = new HashSet<>();
    private final EndpointPathParser endpointPathParser;
    private final boolean onlyGitDiff;
    private final DependencySearcher dependencySearcher;

    public RestClassBuilder(final EndpointPathParser endpointPathParser,
                            final boolean onlyGitDiff,
                            final DependencySearcher dependencySearcher) {
        this.classModel = new ClassModel();
        this.endpointPathParser = endpointPathParser;
        this.onlyGitDiff = onlyGitDiff;
        this.dependencySearcher = dependencySearcher;
    }

    /**
     * Устанавливает имя класса.
     *
     * @param name имя класса
     * @return builder для цепочки вызовов
     */
    public RestClassBuilder withName(String name) {
        classModel.setName(name);
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
    public ClassModel build() {
        classModel.setMethods(methodModels);
        return classModel;
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
        if (!StringUtils.hasText(classModel.getEndpointPath())) {
            var endpointPath = endpointPathParser.getEndpointPath(classInfo);
            classModel.setEndpointPath(endpointPath);
        }
    }

    /**
     * Извлекает методы класса с аннотациями маппинга.
     *
     * @param classInfo информация о классе
     */
    private void extractClassMethods(ClassInfo classInfo) {
        for (var method : classInfo.getMethodInfo()) {
            var requestMapping = method.getAnnotationInfo(RequestMapping.class);
            if (requestMapping != null) {
                var restMethod = buildRestMethod(method, new MethodModel());
                methodModels.add(restMethod);
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
    private MethodModel buildRestMethod(MethodInfo methodInfo, MethodModel builtMethod) {
        return new RestMethodBuilder(methodInfo, builtMethod, endpointPathParser, onlyGitDiff, dependencySearcher)
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