package dev.parfenov.sowa.schema.plugin.parsers;

import io.github.classgraph.ClassInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Утилиты для поиска и анализа аннотаций на классах и методах.
 * <p>
 * Предоставляет методы для поиска аннотаций как напрямую,
 * так и внутри других аннотаций (мета-аннотации).
 */
public class AnnotationParser {

    private AnnotationParser() {
    }

    /**
     * Ищет аннотацию непосредственно на классе.
     *
     * @param <T>               тип аннотации
     * @param lookingAnnotation класс искомой аннотации
     * @param classInfo         информация о классе
     * @return найденная аннотация или null
     */
    public static <T extends Annotation> T findDirectOnClass(Class<T> lookingAnnotation, ClassInfo classInfo) {
        var annotationInfo = classInfo.getAnnotationInfo(lookingAnnotation);
        return annotationInfo != null
                ? lookingAnnotation.cast(annotationInfo.loadClassAndInstantiate())
                : null;
    }

    /**
     * Ищет аннотацию непосредственно на методе.
     *
     * @param <T>               тип аннотации
     * @param lookingAnnotation класс искомой аннотации
     * @param realMethod        метод для поиска
     * @return найденная аннотация или null
     */
    public static <T extends Annotation> T findDirectOnMethod(Class<T> lookingAnnotation, Method realMethod) {
        for (var annotation : realMethod.getAnnotations()) {
            if (lookingAnnotation.isAssignableFrom(annotation.annotationType())) {
                return lookingAnnotation.cast(annotation);
            }
        }
        return null;
    }

    /**
     * Ищет аннотацию на методе напрямую или внутри других аннотаций.
     *
     * @param <T>               тип аннотации
     * @param lookingAnnotation класс искомой аннотации
     * @param realMethod        метод для поиска
     * @return найденная аннотация или null
     */
    public static <T extends Annotation> T directOnMethodOrAnnotations(Class<T> lookingAnnotation, Method realMethod) {
        return Optional
                .ofNullable(findDirectOnMethod(lookingAnnotation, realMethod))
                .orElseGet(() -> findInsideAnyAnnotationOnMethod(lookingAnnotation, realMethod));
    }

    /**
     * Ищет аннотацию внутри других аннотаций на методе (мета-аннотации).
     *
     * @param <T>               тип аннотации
     * @param lookingAnnotation класс искомой аннотации
     * @param method            метод для поиска
     * @return найденная мета-аннотация или null
     */
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
