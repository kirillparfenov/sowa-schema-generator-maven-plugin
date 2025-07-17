package dev.parfenov.sowa.schema.plugin.parsers.classes;

import dev.parfenov.sowa.schema.plugin.classloader.ClassLoader;
import dev.parfenov.sowa.schema.plugin.git.GitDiffParser;
import dev.parfenov.sowa.schema.plugin.parsers.AnnotationParser;
import dev.parfenov.sowa.schema.plugin.parsers.EndpointPathParser;
import dev.parfenov.sowa.schema.plugin.parsers.TypesParser;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.PathVariableInfo;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClassMethod;
import io.github.classgraph.ClassInfo;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassParser {

    private final ClassLoader classLoader;
    private final EndpointPathParser endpointPathParser;
    private final TypesParser typesParser = new TypesParser();
    private final ClassParserConfig config;

    public ClassParser(final ClassParserConfig classParserConfig) {
        this.classLoader = new ClassLoader(classParserConfig);
        this.endpointPathParser = new EndpointPathParser(classParserConfig.project());
        this.config = classParserConfig;
    }

    public List<RestClass> parseAllRestClasses() {
        try (var scanResult = classLoader.getClassgraph().scan()) {
            var result =  scanResult
                    .getClassesWithAnyAnnotation(RestController.class, Controller.class)
                    .filter(this::isProjectPackage)
                    .stream()
                    .map(this::parseRestController)
                    .toList();

            if (config.onlyGitDiff()) {
                new GitDiffParser(config.branchDiffWith(), classLoader).setNullForNoDiff(result);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время сканирования графа классов", e);
        }
    }

    private boolean isProjectPackage(ClassInfo controllerClass) {
        return controllerClass
                .getPackageInfo()
                .getName()
                .startsWith(classLoader.baseProjectPackage());
    }

    public RestClass parseRestController(ClassInfo restController) {
        var currentClass = new RestClass();
        currentClass.setName(restController.getSimpleName());

        var currentClassMethods = new HashMap<String, RestClassMethod>();
        resolveRestClass(restController, currentClass, currentClassMethods);

        for (var interfaceInfo : restController.getInterfaces()) {
            resolveRestClass(interfaceInfo, currentClass, currentClassMethods);
        }
        currentClass.setMethods(new ArrayList<>(currentClassMethods.values()));
        return currentClass;
    }

    private void resolveRestClass(ClassInfo restController,
                                  RestClass currentClass,
                                  Map<String, RestClassMethod> currentClassMethods) {
        if (!StringUtils.hasText(currentClass.getEndpointPath())) {
            var classEndpointPath = classEndpointPath(restController, currentClass.getEndpointPath());
            currentClass.setEndpointPath(classEndpointPath);
        }
        resolveClassMethods(restController, currentClassMethods);
    }

    private String classEndpointPath(ClassInfo classInfo, String currentPath) {
        return StringUtils.hasText(currentPath)
                ? ""
                : endpointPathParser.pathOnClass(classInfo);
    }

    private void resolveClassMethods(ClassInfo classInfo, Map<String, RestClassMethod> methodMap) {
        var resolvedClass = typesParser.resolveErasedType(classInfo.loadClass());
        var classMembers = typesParser.resolveTypeMembers(resolvedClass);
        for (var method : classMembers.getMemberMethods()) {
            var requestMapping = AnnotationParser.directOnMethodOrAnnotations(RequestMapping.class, method.getRawMember());
            if (requestMapping != null) {
                var mapKey = resolveMethodName(method.getRawMember());
                var mapValue = methodMap.getOrDefault(mapKey, new RestClassMethod());
                var extractedMethod = buildRestMethod(method.getRawMember(), mapValue);
                methodMap.put(mapKey, extractedMethod);
            }
        }
    }

    private String resolveMethodName(Method method) {
        var methodName = method.getName();
        var methodBytes = SerializationUtils.serialize(method.toString());
        var md5 = DigestUtils.md5DigestAsHex(methodBytes != null ? methodBytes : methodName.getBytes());
        return methodName + "_" + md5;
    }

    private RestClassMethod buildRestMethod(Method realMethod, RestClassMethod fromMap) {
        var classMethod = Optional.ofNullable(fromMap).orElseGet(RestClassMethod::new);
        classMethod.setName(realMethod.getName());

        if (CollectionUtils.isEmpty(classMethod.getPathVariables())) {
            var pathVariables = extractPathVariables(realMethod);
            classMethod.setPathVariables(pathVariables);
        }

        if (classMethod.getRequest() == null) {
            classMethod.setRequest(extractRequest(realMethod));
        }

        if (classMethod.getResponse() == null) {
            classMethod.setResponse(extractResponse(realMethod));
        }

        if (!StringUtils.hasText(classMethod.getEndpointPath())) {
            var endpointPath = endpointPathParser.pathOnMethod(realMethod);
            classMethod.setEndpointPath(endpointPath);
        }

        if (classMethod.getHttpMethod() == null) {
            var requestMapping = AnnotationParser.directOnMethodOrAnnotations(RequestMapping.class, realMethod);
            if (requestMapping != null && requestMapping.method().length > 0) {
                classMethod.setHttpMethod(requestMapping.method()[0].asHttpMethod());
            }
        }

        return classMethod;
    }

    private List<PathVariableInfo> extractPathVariables(Method realMethod) {
        return Arrays.stream(realMethod.getParameters())
                .filter(param -> param.isAnnotationPresent(PathVariable.class))
                .map(this::buildPathVariable)
                .collect(Collectors.toList());
    }

    private PathVariableInfo buildPathVariable(Parameter parameter) {
        var annotation = parameter.getAnnotation(PathVariable.class);
        var pathValue = Stream.of(annotation.value(), annotation.name())
                .filter(Predicate.not(String::isBlank))
                .findFirst()
                .orElse(parameter.getName());
        return new PathVariableInfo(pathValue, parameter.getType());
    }

    private Type extractRequest(Method realMethod) {
        return Arrays.stream(realMethod.getParameters())
                .filter(param -> param.isAnnotationPresent(RequestBody.class))
                .findFirst()
                .map(Parameter::getParameterizedType)
                .orElse(null);
    }

    private Type extractResponse(Method realMethod) {
        return realMethod.getGenericReturnType();
    }
}
