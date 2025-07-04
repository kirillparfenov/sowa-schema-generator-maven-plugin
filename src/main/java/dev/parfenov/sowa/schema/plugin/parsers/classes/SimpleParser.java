package dev.parfenov.sowa.schema.plugin.parsers.classes;

import dev.parfenov.sowa.schema.plugin.classloader.ClassLoader;
import dev.parfenov.sowa.schema.plugin.classloader.ResolvedClassLoader;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.PathVariableInfo;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClassMethod;
import io.github.classgraph.ClassInfo;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.util.SerializationUtils;
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

public class SimpleParser implements ClassParser {

    private final ClassLoader classLoader;
    private final EndpointPathResolver endpointPathResolver;
    private final ResolvedClassLoader resolvedClassLoader = new ResolvedClassLoader();

    public SimpleParser(final ClassParserConfig classParserConfig) {
        this.classLoader = new ClassLoader(classParserConfig);
        this.endpointPathResolver = new EndpointPathResolver(classParserConfig.project());
    }

    @Override
    public List<RestClass> parseAllRestClasses() {
        try (var scanResult = classLoader.getClassgraph().scan()) {
            return scanResult
                    .getClassesWithAnyAnnotation(RestController.class, Controller.class)
                    .filter(this::isProjectPackage)
                    .stream()
                    .map(this::parseRestController)
                    .toList();
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
        var classEndpointPath = classEndpointPath(restController, currentClass.getEndpointPath());
        if (currentClass.getEndpointPath() == null || currentClass.getEndpointPath().isBlank()) {
            currentClass.setEndpointPath(classEndpointPath);
        }
        resolveClassMethods(restController, currentClassMethods);
    }

    private String classEndpointPath(ClassInfo classInfo, String currentPath) {
        return currentPath == null || currentPath.isBlank()
                ? endpointPathResolver.pathOnClass(classInfo)
                : "";
    }

    private void resolveClassMethods(ClassInfo classInfo, Map<String, RestClassMethod> methodMap) {
        var resolvedClass = resolvedClassLoader.resolveErasedType(classInfo.loadClass());
        var classMembers = resolvedClassLoader.resolveTypeMembers(resolvedClass);
        for (var method : classMembers.getMemberMethods()) {
            var requestMapping = AnnotationResolver.directOnMethodOrAnnotations(RequestMapping.class, method.getRawMember());
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

        if (classMethod.getPathVariables() == null || classMethod.getPathVariables().isEmpty()) {
            var pathVariables = extractPathVariables(realMethod);
            classMethod.setPathVariables(pathVariables);
        }

        if (classMethod.getRequest() == null) {
            classMethod.setRequest(extractRequest(realMethod));
        }

        if (classMethod.getResponse() == null) {
            classMethod.setResponse(extractResponse(realMethod));
        }

        if (classMethod.getEndpointPath() == null || classMethod.getEndpointPath().isBlank()) {
            var endpointPath = endpointPathResolver.pathOnMethod(realMethod);
            classMethod.setEndpointPath(endpointPath);
        }

        if (classMethod.getHttpMethod() == null) {
            var requestMapping = AnnotationResolver.directOnMethodOrAnnotations(RequestMapping.class, realMethod);
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
