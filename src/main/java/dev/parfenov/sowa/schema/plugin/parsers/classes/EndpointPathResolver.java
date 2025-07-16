package dev.parfenov.sowa.schema.plugin.parsers.classes;

import dev.parfenov.sowa.schema.plugin.generator.Regex;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClassMethod;
import dev.parfenov.sowa.schema.plugin.parsers.properties.PropertiesParser;
import io.github.classgraph.ClassInfo;
import org.apache.maven.project.MavenProject;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;


public class EndpointPathResolver {

    private final MavenProject mavenProject;

    public EndpointPathResolver(final MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    public String resolvePathWithVariables(RestClass restClass, RestClassMethod restMethod) {
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

    public String contextPath() {
        return PropertiesParser.contextPath(mavenProject);
    }

    public String endpointToSchema(RestClass restClass, RestClassMethod restMethod) {
        return restClass.getName().concat("_").concat(restMethod.getName());
    }

    public String pathOnClass(ClassInfo controllerClass) {
        var requestMapping = AnnotationResolver.findDirectOnClass(RequestMapping.class, controllerClass);
        if (requestMapping != null) {
            if (requestMapping.value().length > 0) {
                return cleanSlashes(requestMapping.value()[0]);
            }
        }
        return "";
    }

    public String pathOnMethod(Method method) {
        var requestMapping = AnnotationResolver.findDirectOnMethod(RequestMapping.class, method);
        if (requestMapping != null) {
            if (requestMapping.value().length > 0) {
                return cleanSlashes(requestMapping.value()[0]);
            }
        }

        var getMapping = AnnotationResolver.findDirectOnMethod(GetMapping.class, method);
        if (getMapping != null) {
            if (getMapping.value().length > 0) {
                return cleanSlashes(getMapping.value()[0]);
            }
        }

        var postMapping = AnnotationResolver.findDirectOnMethod(PostMapping.class, method);
        if (postMapping != null) {
            if (postMapping.value().length > 0) {
                return cleanSlashes(postMapping.value()[0]);
            }
        }

        var putMapping = AnnotationResolver.findDirectOnMethod(PutMapping.class, method);
        if (putMapping != null) {
            if (putMapping.value().length > 0) {
                return cleanSlashes(putMapping.value()[0]);
            }
        }

        var deleteMapping = AnnotationResolver.findDirectOnMethod(DeleteMapping.class, method);
        if (deleteMapping != null) {
            if (deleteMapping.value().length > 0) {
                return cleanSlashes(deleteMapping.value()[0]);
            }
        }

        var patchMapping = AnnotationResolver.findDirectOnMethod(PatchMapping.class, method);
        if (patchMapping != null) {
            if (patchMapping.value().length > 0) {
                return cleanSlashes(patchMapping.value()[0]);
            }
        }

        return "";
    }

    private String cleanSlashes(String path) {
        if (path.isBlank()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
