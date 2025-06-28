package dev.parfenov.sowa.schema.plugin.parsers.classes;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import io.github.classgraph.ClassInfo;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AnnotationResolver {

    public static final Set<String> ENDPOINT_ANNOTATIONS_STRINGS = Set.of(
            GetMapping.class.getName(),
            PostMapping.class.getName(),
            PutMapping.class.getName(),
            DeleteMapping.class.getName(),
            PatchMapping.class.getName(),
            RequestMapping.class.getName()
    );

    private AnnotationResolver() {}

    public static <T extends Annotation> T findDirectOnClass(Class<T> lookingAnnotation, ClassInfo classInfo) {
        var annotationInfo = classInfo.getAnnotationInfo(lookingAnnotation);
        return annotationInfo != null
                ? lookingAnnotation.cast(annotationInfo.loadClassAndInstantiate())
                : null;
    }

    public static <T extends Annotation> T findDirectOnMethod(Class<T> lookingAnnotation, ResolvedMethod resolvedMethod) {
        for (var annotation : resolvedMethod.getAnnotations()) {
            if (lookingAnnotation.isAssignableFrom(annotation.annotationType())) {
                return lookingAnnotation.cast(annotation);
            }
        }
        return null;
    }

    public static <T extends Annotation> T findDirectOnParameter(Class<T> lookingAnnotation, Parameter parameter) {
        for (var annotation : parameter.getAnnotations()) {
            if (lookingAnnotation.isAssignableFrom(annotation.annotationType())) {
                return lookingAnnotation.cast(annotation);
            }
        }
        return null;
    }

    public static <T extends Annotation> T findInsideAnyAnnotationOnMethod(Class<T> lookingAnnotation, ResolvedMethod method) {
        for (var annotation : method.getAnnotations()) {
            for (var internalAnnotation : annotation.annotationType().getDeclaredAnnotations()) {
                if (lookingAnnotation.isAssignableFrom(internalAnnotation.annotationType())) {
                    return lookingAnnotation.cast(internalAnnotation);
                }
            }
        }
        return null;
    }

    public static <T extends Annotation> List<ResolvedType> methodParamsWithAnnotation(Class<T> lookingAnnotation, ResolvedMethod method) {
        var params = new ArrayList<ResolvedType>();
        for (int i = 0; i < method.getArgumentCount(); i++) {
            for (var annotation : method.getParameterAnnotations(i)) {
                if (lookingAnnotation.isAssignableFrom(annotation.annotationType())) {
                    params.add(method.getArgumentType(i));
                }
            }
        }
        return params;
    }

    /**
     * Аннотации над методом берутся даже вложенные друг в друга (полезно сразу получить @RequestMapping из @PostMapping и пр.)
     *
     * @param classInfo класс, методы которого парсятся на наличие аннотаций
     * @return true, если хотя бы над одним методом в классе есть аннотация, указанная в annotations
     */
    public static boolean classMethodsHasAnyRestAnnotation(ClassInfo classInfo) {
        var methodAnnotations = classInfo.getMethodAnnotations();
        for (var annotation : methodAnnotations) {
            if (ENDPOINT_ANNOTATIONS_STRINGS.contains(annotation.getName())) return true;
        }
        return false;
    }
}
