package dev.parfenov.sowa.schema.plugin.parsers;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationInfoList;
import io.github.classgraph.MethodInfo;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

/**
 * Утилиты для поиска и анализа аннотаций на классах и методах.
 * <p>
 * Предоставляет методы для поиска аннотаций как напрямую,
 * так и внутри других аннотаций (мета-аннотации).
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class AnnotationParser {

    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping"
    );

    private AnnotationParser() {
    }

    /**
     * Получение value из {@link AnnotationParser#MAPPING_ANNOTATIONS}
     *
     * @param annotations список аннотаций
     * @return значение value, либо пустая строка
     */
    public static String extractPathValue(AnnotationInfoList annotations) {
        for (var annotation : annotations) {
            for (var paramValue : annotation.getParameterValues()) {
                if (paramValue.getName().equals("value") && paramValue.getValue() instanceof String[] value) {
                    if (value.length > 0 && MAPPING_ANNOTATIONS.contains(annotation.getName())) {
                        return value[0];
                    }
                }
            }
        }
        return "";
    }

    /**
     * Получить HTTP-метод, присвоенный методу
     *
     * @param methodInfo метод, над которым ищем HTTP-метод
     * @return HTTP-метод
     */
    public static Optional<HttpMethod> getHttpMethod(MethodInfo methodInfo) {
        return getAnnotation(RequestMapping.class, methodInfo)
                .map(RequestMapping::method)
                .filter(method -> method.length > 0)
                .map(method -> method[0].asHttpMethod());
    }

    /**
     * Ищет аннотацию на методе.
     *
     * @param <T>               тип аннотации
     * @param lookingAnnotation класс искомой аннотации
     * @param method            метод, над которым ищем аннотацию
     * @return найденная аннотация или null
     */
    public static <T extends Annotation> Optional<T> getAnnotation(Class<T> lookingAnnotation, MethodInfo method) {
        return extractAnnotation(
                lookingAnnotation,
                method.getAnnotationInfo(lookingAnnotation)

        );
    }

    /**
     * Получить искомую аннотацию
     *
     * @param <A>               тип искомой аннотации
     * @param lookingAnnotation класс искомой аннотации
     * @param annotationInfo    найденная аннотация, либо null
     * @return загруженная аннотация с данными
     */
    private static <A extends Annotation> Optional<A> extractAnnotation(Class<A> lookingAnnotation, AnnotationInfo annotationInfo) {
        return Optional
                .ofNullable(annotationInfo)
                .map(annotation -> lookingAnnotation.cast(annotation.loadClassAndInstantiate()));
    }
}
