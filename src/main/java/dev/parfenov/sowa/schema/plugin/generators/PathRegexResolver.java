package dev.parfenov.sowa.schema.plugin.generators;

import com.fasterxml.classmate.TypeResolver;
import dev.parfenov.sowa.schema.plugin.generators.handlers.ConstraintResolver;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Паттерны регулярных выражений для различных типов данных.
 * <p>
 * Предоставляет соответствие между Java классами и их regex представлениями
 * для валидации в путях URL.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
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
        } else if (ConstraintResolver.getNumberClass(resolvedType).isPresent()) {
            return numberRegex();
        }
        return DEFAULT_REGEX;
    }

    public static String uidRegex() {
        return "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    }

    public static String enumRegex(Enum<? extends Enum<?>>[] values) {
        var joiner = new StringJoiner("|", "(", ")");
        Arrays.stream(values)
                .map(Enum::name)
//                .peek(joiner::add)
//                .map(String::toLowerCase)
                .forEach(joiner::add);

        return joiner.toString();
    }

    public static String numberRegex() {
        return "\\d+";
    }
}
