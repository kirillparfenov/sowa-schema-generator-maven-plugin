package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.generator.Regex;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;
import io.github.classgraph.ClassInfo;
import org.apache.maven.project.MavenProject;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

/**
 * Парсер путей эндпоинтов Spring MVC.
 * <p>
 * Извлекает и обрабатывает пути из аннотаций Spring MVC,
 * разрешает переменные пути и формирует полные URL.
 */
public class EndpointPathParser {

    private final MavenProject mavenProject;

    /**
     * Создает парсер путей.
     *
     * @param mavenProject Maven проект для получения контекстного пути
     */
    public EndpointPathParser(final MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    /**
     * Разрешает полный путь с переменными в regex формат.
     *
     * @param restClass  REST контроллер
     * @param restMethod метод контроллера
     * @return полный путь с regex паттернами для переменных пути
     */
    public String resolvePathWithVariables(RestClass restClass, RestMethod restMethod) {
        var fullPath = "^/proxy" + contextPath() + restClass.getEndpointPath() + restMethod.getEndpointPath() + "$";
        var pathVariables = restMethod.getPathVariables();
        if (pathVariables == null || pathVariables.isEmpty()) {
            return fullPath;
        }

        for (var pathVariable : pathVariables) {
            var name = "\\{" + pathVariable.getParamName() + "}";
            var typeName = pathVariable.getParamType().getTypeName();
            var regex = Regex.getRegexOrDefault(typeName);
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
     * Генерирует имя схемы на основе класса и метода.
     *
     * @param restClass  REST контроллер
     * @param restMethod метод контроллера
     * @return имя схемы в формате "ClassName_methodName"
     */
    public String endpointToSchema(RestClass restClass, RestMethod restMethod) {
        return restClass.getName().concat("_").concat(restMethod.getName());
    }

    /**
     * Извлекает путь из аннотации @RequestMapping на классе.
     *
     * @param controllerClass информация о классе контроллера
     * @return путь из аннотации или пустая строка
     */
    public String pathOnClass(ClassInfo controllerClass) {
        var requestMapping = AnnotationParser.findDirectOnClass(RequestMapping.class, controllerClass);
        if (requestMapping != null) {
            if (requestMapping.value().length > 0) {
                return cleanSlashes(requestMapping.value()[0]);
            }
        }
        return "";
    }

    /**
     * Извлекает путь из аннотаций маппинга на методе.
     * <p>
     * Поддерживает @RequestMapping, @GetMapping, @PostMapping,
     *
     * @param method метод для анализа
     * @return путь из аннотации или пустая строка
     * @PutMapping, @DeleteMapping, @PatchMapping.
     */
    public String pathOnMethod(Method method) {
        var requestMapping = AnnotationParser.findDirectOnMethod(RequestMapping.class, method);
        if (requestMapping != null) {
            if (requestMapping.value().length > 0) {
                return cleanSlashes(requestMapping.value()[0]);
            }
        }

        var getMapping = AnnotationParser.findDirectOnMethod(GetMapping.class, method);
        if (getMapping != null) {
            if (getMapping.value().length > 0) {
                return cleanSlashes(getMapping.value()[0]);
            }
        }

        var postMapping = AnnotationParser.findDirectOnMethod(PostMapping.class, method);
        if (postMapping != null) {
            if (postMapping.value().length > 0) {
                return cleanSlashes(postMapping.value()[0]);
            }
        }

        var putMapping = AnnotationParser.findDirectOnMethod(PutMapping.class, method);
        if (putMapping != null) {
            if (putMapping.value().length > 0) {
                return cleanSlashes(putMapping.value()[0]);
            }
        }

        var deleteMapping = AnnotationParser.findDirectOnMethod(DeleteMapping.class, method);
        if (deleteMapping != null) {
            if (deleteMapping.value().length > 0) {
                return cleanSlashes(deleteMapping.value()[0]);
            }
        }

        var patchMapping = AnnotationParser.findDirectOnMethod(PatchMapping.class, method);
        if (patchMapping != null) {
            if (patchMapping.value().length > 0) {
                return cleanSlashes(patchMapping.value()[0]);
            }
        }

        return "";
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
