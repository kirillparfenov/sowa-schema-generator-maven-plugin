package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.parsers.dto.PathVariableInfo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Утилитный класс для извлечения информации из Java методов.
 * <p>
 * Предоставляет статические методы для извлечения:
 * - переменных пути (@PathVariable)
 * - типов запросов (@RequestBody)
 * - типов ответов (возвращаемый тип метода)
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public final class MethodExtractor {

    private MethodExtractor() {
    }

    /**
     * Извлекает переменные пути из параметров метода.
     *
     * @param method Java метод для анализа
     * @return список переменных пути
     */
    public static List<PathVariableInfo> extractPathVariables(Method method) {
        return Arrays.stream(method.getParameters())
                .filter(param -> param.isAnnotationPresent(PathVariable.class))
                .map(MethodExtractor::buildPathVariable)
                .collect(Collectors.toList());
    }

    /**
     * Извлекает тип запроса из параметров метода.
     *
     * @param method Java метод для анализа
     * @return тип запроса или null если не найден
     */
    public static Type extractRequest(Method method) {
        return Arrays.stream(method.getParameters())
                .filter(param -> param.isAnnotationPresent(RequestBody.class))
                .findFirst()
                .map(Parameter::getParameterizedType)
                .orElse(null);
    }

    /**
     * Извлекает тип ответа метода.
     *
     * @param method Java метод для анализа
     * @return тип ответа метода
     */
    public static Type extractResponse(Method method) {
        return method.getGenericReturnType();
    }

    /**
     * Строит объект переменной пути из параметра метода.
     *
     * @param parameter параметр метода с аннотацией @PathVariable
     * @return информация о переменной пути
     */
    private static PathVariableInfo buildPathVariable(Parameter parameter) {
        var annotation = parameter.getAnnotation(PathVariable.class);
        var pathValue = Stream.of(annotation.value(), annotation.name())
                .filter(Predicate.not(String::isBlank))
                .findFirst()
                .orElse(parameter.getName());
        return new PathVariableInfo(pathValue, parameter.getType());
    }
} 