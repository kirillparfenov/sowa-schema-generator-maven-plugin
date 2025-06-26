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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractClassParser implements ClassParser {

    protected static final Set<Class<? extends Annotation>> ENDPOINT_ANNOTATIONS = Set.of(
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class,
            RequestMapping.class
    );

    protected final ClassLoader classLoader;
    protected final ClassParserConfig config;
    protected final ResolvedClassLoader resolvedClassLoader = new ResolvedClassLoader();

    protected AbstractClassParser(final ClassParserConfig config) {
        this.classLoader = new ClassLoader(config.project());
        this.config = config;
    }

    /**
     * @return все методы из всех {@link RestController}
     */
    @Override
    public List<ClassMethod> getAllRestControllersMethods() {
        try (var scanResult = classLoader.getClassgraph().scan()) {
            var restControllerClasses = scanResult.getClassesWithAnyAnnotation(RestController.class, Controller.class);
            return collectRestControllerMethods(restControllerClasses);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время сканирования графа классов", e);
        }
    }

    private List<ClassMethod> collectRestControllerMethods(ClassInfoList restControllerClasses) {
        var allRestControllerMethods = new ArrayList<ClassMethod>();
        for (var restController : restControllerClasses) {
            if (skipClassParsing(restController)) {
                continue;
            }
            var restClass = classLoader.loadErasedClass(restController.getName());
            var restClassType = resolvedClassLoader.resolveErasedType(restClass);
            var restClassMethods = resolvedClassLoader.resolveTypeMembers(restClassType).getMemberMethods();
            allRestControllerMethods.addAll(collectRestControllerMethods(restClassMethods, restController));
        }
        return allRestControllerMethods;
    }

    private boolean skipClassParsing(ClassInfo controllerClass) {
        return notProjectPackage(controllerClass) || noRestAnnotations(controllerClass);
    }

    private boolean noRestAnnotations(ClassInfo controllerClass) {
        for (var method : controllerClass.getMethodInfo()) {
            for (var restAnnotation : ENDPOINT_ANNOTATIONS) {
                if (method.hasAnnotation(restAnnotation)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean notProjectPackage(ClassInfo controllerClass) {
        return !controllerClass
                .getPackageInfo()
                .getName()
                .startsWith(classLoader.baseProjectPackage());
    }

    /**
     * Получение информации из переданных методов контроллера
     *
     * @param restMethods    методы контроллера
     * @param restController контроллер
     * @return методы конкретного контроллера
     */
    private List<ClassMethod> collectRestControllerMethods(ResolvedMethod[] restMethods, ClassInfo restController) {
        var methods = new ArrayList<ClassMethod>();
        for (var restMethod : restMethods) {
            var restControllerName = restController.getSimpleName().concat("_").concat(restMethod.getName());
            var response = restMethod.getReturnType();
            var request = getRequest(restMethod);
            var httpMethod = getHttpMethod(restMethod);
            var endpointPath = getEndpointUrl(restController, restMethod);
            methods.add(new ClassMethod(restControllerName, request, response, httpMethod, endpointPath));
        }
        return methods;
    }

    /**
     * Получение тела запроса в методе. Аннотируется {@link RequestBody}
     *
     * @param method расширенная информация о методе в контроллере
     */
    private ResolvedType getRequest(ResolvedMethod method) {
        for (int i = 0; i < method.getArgumentCount(); i++) {
            for (var annotation : method.getParameterAnnotations(i)) {
                if (annotation.annotationType().equals(RequestBody.class)) {
                    return method.getArgumentType(i);
                }
            }
        }
        return null;
    }

    /**
     * Поиск аннотаций {@link GetMapping}, {@link PostMapping} и т.д. для получение {@link HttpMethod}
     *
     * @param method расширенная информация о методе в контроллере
     */
    private HttpMethod getHttpMethod(ResolvedMethod method) {
        for (var annotation : method.getAnnotations()) {
            for (var internalAnnotation : annotation.annotationType().getDeclaredAnnotations()) {
                if (internalAnnotation instanceof RequestMapping requestMapping) {
                    return requestMapping.method()[0].asHttpMethod();
                }
            }
        }
        return null;
    }

    private String getEndpointUrl(ClassInfo restControllerClass, ResolvedMethod method) {
        var controllerUrl = getControllerUrl(restControllerClass);
        for (var annotation : method.getAnnotations()) {
            if (ENDPOINT_ANNOTATIONS.contains(annotation.annotationType())) {
                if (annotation instanceof RequestMapping requestMapping) {
                    if (requestMapping.value().length == 0) {
                        return controllerUrl;
                    }
                    return controllerUrl + requestMapping.value()[0];
                } else if (annotation instanceof GetMapping getMapping) {
                    if (getMapping.value().length == 0) {
                        return controllerUrl;
                    }
                    return controllerUrl + getMapping.value()[0];
                } else if (annotation instanceof PostMapping postMapping) {
                    if (postMapping.value().length == 0) {
                        return controllerUrl;
                    }
                    return controllerUrl + postMapping.value()[0];
                } else if (annotation instanceof PutMapping putMapping) {
                    if (putMapping.value().length == 0) {
                        return controllerUrl;
                    }
                    return controllerUrl + putMapping.value()[0];
                } else if (annotation instanceof DeleteMapping deleteMapping) {
                    if (deleteMapping.value().length == 0) {
                        return controllerUrl;
                    }
                    return controllerUrl + deleteMapping.value()[0];
                } else if (annotation instanceof PatchMapping patchMapping) {
                    if (patchMapping.value().length == 0) {
                        return controllerUrl;
                    }
                    return controllerUrl + patchMapping.value()[0];
                }
            }
        }
        return controllerUrl;
    }

    private String getControllerUrl(ClassInfo restControllerClass) {
        for (var annotation : restControllerClass.getAnnotations()) {
            if (annotation instanceof RestController restController) {
                return restController.value();
            } else if (annotation instanceof Controller controller) {
                return controller.value();
            }
        }
        return "";
    }
}
