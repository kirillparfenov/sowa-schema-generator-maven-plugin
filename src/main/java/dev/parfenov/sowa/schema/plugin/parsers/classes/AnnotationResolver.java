package dev.parfenov.sowa.schema.plugin.parsers.classes;

import io.github.classgraph.ClassInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

public class AnnotationResolver {

    private AnnotationResolver() {
    }

    public static <T extends Annotation> T findDirectOnClass(Class<T> lookingAnnotation, ClassInfo classInfo) {
        var annotationInfo = classInfo.getAnnotationInfo(lookingAnnotation);
        return annotationInfo != null
                ? lookingAnnotation.cast(annotationInfo.loadClassAndInstantiate())
                : null;
    }

    public static <T extends Annotation> T findDirectOnMethod(Class<T> lookingAnnotation, Method realMethod) {
        for (var annotation : realMethod.getAnnotations()) {
            if (lookingAnnotation.isAssignableFrom(annotation.annotationType())) {
                return lookingAnnotation.cast(annotation);
            }
        }
        return null;
    }

    public static <T extends Annotation> T directOnMethodOrAnnotations(Class<T> lookingAnnotation, Method realMethod) {
        return Optional
                .ofNullable(findDirectOnMethod(lookingAnnotation, realMethod))
                .orElseGet(() -> findInsideAnyAnnotationOnMethod(lookingAnnotation, realMethod));
    }

    public static <T extends Annotation> T findDirectOnParameter(Class<T> lookingAnnotation, Parameter parameter) {
        for (var annotation : parameter.getAnnotations()) {
            if (lookingAnnotation.isAssignableFrom(annotation.annotationType())) {
                return lookingAnnotation.cast(annotation);
            }
        }
        return null;
    }

    public static <T extends Annotation> T findInsideAnyAnnotationOnMethod(Class<T> lookingAnnotation, Method method) {
        for (var annotation : method.getAnnotations()) {
            for (var internalAnnotation : annotation.annotationType().getDeclaredAnnotations()) {
                if (lookingAnnotation.isAssignableFrom(internalAnnotation.annotationType())) {
                    return lookingAnnotation.cast(internalAnnotation);
                }
            }
        }
        return null;
    }
}
