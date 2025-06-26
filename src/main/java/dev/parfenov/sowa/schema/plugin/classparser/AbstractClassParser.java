package dev.parfenov.sowa.schema.plugin.classparser;

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
            var foundRestMethods = scanResult.getClassesWithAnyAnnotation(RestController.class, Controller.class);
            return collectRestControllerMethods(foundRestMethods);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время сканирования графа классов", e);
        }
    }

    private List<ClassMethod> collectRestControllerMethods(ClassInfoList classInfoList) {
        var allRestControllerMethods = new ArrayList<ClassMethod>();
        for (var restController : classInfoList) {
            if (!restController.getPackageInfo().getName().startsWith(classLoader.baseProjectPackage())) {
                continue;
            }
            var restClass = classLoader.loadErasedClass(restController.getName());
            var restClassType = resolvedClassLoader.resolveErasedType(restClass);
            var restClassMethods = resolvedClassLoader.resolveTypeMembers(restClassType).getMemberMethods();
            allRestControllerMethods.addAll(collectRestControllerMethods(restClassMethods, restController));
        }
        return allRestControllerMethods;
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
            methods.add(new ClassMethod(restControllerName, request, response, httpMethod));
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
}
