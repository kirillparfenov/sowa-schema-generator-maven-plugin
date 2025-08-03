package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.generators.PathRegexResolver;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import org.apache.maven.project.MavenProject;
import org.springframework.util.CollectionUtils;

/**
 * Парсер путей эндпоинтов Spring MVC.
 * <p>
 * Извлекает и обрабатывает пути из аннотаций Spring MVC,
 * разрешает переменные пути и формирует полные URL.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class EndpointPathParser {

    private final MavenProject mavenProject;

    public EndpointPathParser(final MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    /**
     * Разрешает полный путь с переменными в regex формат.
     *
     * @param classModel  REST контроллер
     * @param methodModel метод контроллера
     * @return полный путь с regex паттернами для переменных пути
     */
    public String resolvePathWithVariables(ClassModel classModel, MethodModel methodModel) {
        var fullPath = contextPath() + classModel.getEndpointPath() + methodModel.getEndpointPath() + "$";
        var pathVariables = methodModel.getPathVariables();
        if (CollectionUtils.isEmpty(pathVariables)) {
            return fullPath;
        }

        for (var pathVariable : pathVariables) {
            var name = "\\{" + pathVariable.getParamName() + "}";
            var regex = PathRegexResolver.getRegexOrDefault(pathVariable.getParamType());
            fullPath = fullPath.replaceAll(name, regex);
        }
        return fullPath;
    }

    /**
     * Получает контекстный путь приложения.
     *
     * @return контекстный путь из конфигурации проекта
     */
    public String contextPath() {
        return PropertiesParser.contextPath(mavenProject);
    }

    /**
     * Получить HTTP-путь класса
     *
     * @param classInfo класс, над которым ищем HTTP-путь
     * @return HTTP-путь
     */
    public String getEndpointPath(ClassInfo classInfo) {
        return cleanSlashes(AnnotationParser.extractPathValue(classInfo.getAnnotationInfo()));
    }

    /**
     * Получить HTTP-путь метода
     *
     * @param methodInfo метод, над которым ищем HTTP-путь
     * @return HTTP-путь
     */
    public String getEndpointPath(MethodInfo methodInfo) {
        return cleanSlashes(AnnotationParser.extractPathValue(methodInfo.getAnnotationInfo()));
    }

    /**
     * Очищает слэши в пути, добавляя начальный слэш если необходимо.
     *
     * @param path путь для очистки
     * @return нормализованный путь
     */
    private String cleanSlashes(String path) {
        if (path.isBlank()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
