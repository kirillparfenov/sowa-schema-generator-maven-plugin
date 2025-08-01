/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.generators;

import com.fasterxml.classmate.TypeResolver;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Паттерны регулярных выражений для различных типов данных.
 * <p>
 * Предоставляет соответствие между Java классами и их regex представлениями
 * для валидации в путях URL.
 */
public class PathRegexResolver {

    private static final String DEFAULT_REGEX = ".{0,255}";

    private PathRegexResolver() {
    }

    /**
     * Возвращает regex паттерн для указанного класса.
     *
     * @param type erased - тип
     * @return regex паттерн для класса или паттерн по умолчанию
     */
    public static String getRegexOrDefault(Type type) {
        var resolvedType = new TypeResolver().resolve(type);
        var clazz = resolvedType.getErasedType();
        if (clazz.isAssignableFrom(UUID.class)) {
            return uidRegex();
        } else if (clazz.isEnum()) {
            var values = ((Class<? extends Enum<?>>) clazz).getEnumConstants();
            return enumRegex(values);
        }
        return DEFAULT_REGEX;
    }

    private static String uidRegex() {
        return "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    }

    private static String enumRegex(Enum<? extends Enum<?>>[] values) {
        var joiner = new StringJoiner("|", "(", ")");
        Arrays.stream(values)
                .map(Enum::name)
//                .peek(joiner::add)
//                .map(String::toLowerCase)
                .forEach(joiner::add);

        return joiner.toString();
    }
}
