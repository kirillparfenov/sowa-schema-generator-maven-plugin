package dev.parfenov.sowa.schema.plugin.parsers.classes;

import com.fasterxml.classmate.members.ResolvedMethod;
import dev.parfenov.sowa.schema.plugin.parsers.properties.PropertiesParser;
import io.github.classgraph.ClassInfo;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


public class EndpointPathResolver {

    private final ClassParserConfig config;

    public EndpointPathResolver(ClassParserConfig config) {
        this.config = config;
    }

    public List<PathVariableParam> pathVariableArguments(ResolvedMethod method) {
        var pathVariableArguments = new ArrayList<PathVariableParam>();
        for (var parameter : method.getRawMember().getParameters()) {
            var pathVariable = AnnotationResolver.findDirectOnParameter(PathVariable.class, parameter);
            if (pathVariable != null) {
                var paramName = pathVariable.value().isBlank()
                        ? parameter.getName()
                        : pathVariable.value();
                pathVariableArguments.add(new PathVariableParam(paramName, parameter.getType()));
            }
        }
        return pathVariableArguments;
    }

    public String resolve(ClassInfo controllerClass, ResolvedMethod method) {
        var contextPath = contextPath();
        var pathOnClass = pathOnClass(controllerClass);
        var pathOnMethod = pathOnMethod(method);
        return contextPath.concat(pathOnClass).concat(pathOnMethod);
    }

    private String contextPath() {
        return PropertiesParser.contextPath(config.project());
    }

    private String pathOnClass(ClassInfo controllerClass) {
        var requestMapping = AnnotationResolver.findDirectOnClass(RequestMapping.class, controllerClass);
        if (requestMapping != null) {
            //todo добавить обработку когда интерфейс extends интерфейс и там @RequestMapping
            if (requestMapping.value().length > 0) {
                return cleanSlashes(requestMapping.value()[0]);
            }
        }
        return "";
    }

    private String pathOnMethod(ResolvedMethod method) {
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
