package dev.parfenov.sowa.schema.plugin.parsers.classes;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import dev.parfenov.sowa.schema.plugin.classloader.ClassLoader;
import dev.parfenov.sowa.schema.plugin.classloader.ResolvedClassLoader;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractClassParser implements ClassParser {

    protected final ClassLoader classLoader;
    protected final ClassParserConfig config;
    protected final EndpointPathResolver endpointPathResolver;
    protected final ResolvedClassLoader resolvedClassLoader = new ResolvedClassLoader();

    protected AbstractClassParser(final ClassParserConfig config) {
        this.classLoader = new ClassLoader(config);
        this.config = config;
        this.endpointPathResolver = new EndpointPathResolver(config);
    }

    /**
     * @return все методы из всех {@link RestController}
     */
    @Override
    public List<ClassMethod> findAllRestControllerMethods() {
        try (var scanResult = classLoader.getClassgraph().scan()) {
            var restControllerClasses =
                    scanResult
                            .getClassesWithAnyAnnotation(RestController.class, Controller.class)
                            .filter(this::pass);
            return collectRestControllerMethods(restControllerClasses);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время сканирования графа классов", e);
        }
    }

    private List<ClassMethod> collectRestControllerMethods(ClassInfoList restControllerClasses) {
        var allRestControllerMethods = new ArrayList<ClassMethod>();
        for (var restController : restControllerClasses) {
            var restClass = classLoader.loadErasedClass(restController.getName());
            var restClassType = resolvedClassLoader.resolveErasedType(restClass);
            var restClassMethods = resolvedClassLoader.resolveTypeMembers(restClassType).getMemberMethods();
            allRestControllerMethods.addAll(collectRestControllerMethods(restClassMethods, restController));
        }
        return allRestControllerMethods;
    }

    private boolean pass(ClassInfo controllerClass) {
        return isProjectPackage(controllerClass)
                && AnnotationResolver.classMethodsHasAnyRestAnnotation(controllerClass);
    }

    private boolean isProjectPackage(ClassInfo controllerClass) {
        return controllerClass
                .getPackageInfo()
                .getName()
                .startsWith(classLoader.baseProjectPackage());
    }

    /**
     * Получение информации из переданных методов контроллера
     *
     * @param restMethods     методы контроллера
     * @param controllerClass контроллер
     * @return методы конкретного контроллера
     */
    private List<ClassMethod> collectRestControllerMethods(ResolvedMethod[] restMethods, ClassInfo controllerClass) {
        var methods = new ArrayList<ClassMethod>();
        for (var restMethod : restMethods) {
            var endpointName = controllerClass.getSimpleName().concat("_").concat(restMethod.getName());
            var response = restMethod.getReturnType();
            var request = getRequest(restMethod);
            var httpMethod = getHttpMethod(restMethod);
            var endpointPath = endpointPathResolver.resolve(controllerClass, restMethod);
            //todo закончить: (нужно будет для составления регулярок в endpointPath)
            var pathVariables = endpointPathResolver.pathVariableArguments(restMethod);
            methods.add(new ClassMethod(endpointName, request, response, httpMethod, endpointPath, pathVariables));
        }
        return methods;
    }

    /**
     * Получение тела запроса в методе. Аннотируется {@link RequestBody}
     *
     * @param method расширенная информация о методе в контроллере
     */
    private ResolvedType getRequest(ResolvedMethod method) {
        var methodParams = AnnotationResolver.methodParamsWithAnnotation(RequestBody.class, method);
        return methodParams.isEmpty() ? null : methodParams.get(0);
    }

    /**
     * Поиск аннотаций {@link GetMapping}, {@link PostMapping} и т.д. для получение {@link HttpMethod}
     *
     * @param method расширенная информация о методе в контроллере
     */
    private HttpMethod getHttpMethod(ResolvedMethod method) {
        var requestMapping = AnnotationResolver.findInsideAnyAnnotationOnMethod(RequestMapping.class, method);
        if (requestMapping != null) {
            return requestMapping.method()[0].asHttpMethod();
        }
        return null;
    }
}
