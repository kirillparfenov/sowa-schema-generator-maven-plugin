package dev.parfenov.sowa.schema.plugin.generator;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.MemberScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static dev.parfenov.sowa.schema.plugin.generator.MemberAnnotationExtractor.getSchemaAnnotationValue;
import static dev.parfenov.sowa.schema.plugin.generator.ValidationConstants.DEFAULT_STRING_MAX_LENGTH;
import static dev.parfenov.sowa.schema.plugin.generator.ValidationConstants.UUID_MAX_LENGTH;

/**
 * Предоставляет методы для определения ограничений типов
 * на основе аннотаций Schema и характеристик самих типов.
 */
public final class ConstraintResolver {

    private ConstraintResolver() {
    }

    /**
     * Разрешает максимальную длину строки для типа.
     *
     * @param member           область видимости члена класса
     * @param lengthCalculator калькулятор длины строк
     * @param superResolver    базовый resolver из родительского класса
     * @return максимальная длина строки или null
     */
    public static Integer resolveStringMaxLength(MemberScope<?, ?> member,
                                                 StringLengthCalculator lengthCalculator,
                                                 Function<MemberScope<?, ?>, Integer> superResolver) {
        // Сначала проверяем базовую валидацию
        var maxLength = superResolver.apply(member);
        if (maxLength != null) {
            return lengthCalculator.increaseLength(maxLength);
        }

        // Затем проверяем аннотацию Schema
        maxLength = getSchemaAnnotationValue(
                member,
                Schema::maxLength,
                length -> length < Integer.MAX_VALUE && length > -1
        ).orElse(null);

        if (maxLength != null) {
            return lengthCalculator.increaseLength(maxLength);
        }

        // Определяем длину по типу
        return getDefaultLengthForType(member);
    }

    /**
     * Получает длину по умолчанию для типа.
     *
     * @param member область видимости члена класса
     * @return длина по умолчанию или null
     */
    private static Integer getDefaultLengthForType(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(CharSequence.class)) {
            return DEFAULT_STRING_MAX_LENGTH;
        }

        if (member.getType().isInstanceOf(UUID.class)) {
            return UUID_MAX_LENGTH;
        }

        return null;
    }

    /**
     * Разрешает максимальное значение для числовых типов.
     *
     * @param type разрешенный тип
     * @return максимальное значение или null
     */
    public static BigDecimal resolveNumericMaximum(ResolvedType type) {
        if (type.isInstanceOf(Integer.class) || type.isInstanceOf(int.class)) {
            return new BigDecimal(Integer.MAX_VALUE);
        } else if (type.isInstanceOf(Long.class) || type.isInstanceOf(long.class)) {
            return new BigDecimal(Long.MAX_VALUE);
        } else if (type.isInstanceOf(Byte.class) || type.isInstanceOf(byte.class)) {
            return new BigDecimal(Byte.MAX_VALUE);
        }

        return null;
    }

    /**
     * Разрешает regex паттерн для строковых типов.
     *
     * @param member        область видимости члена класса
     * @param superResolver базовый resolver из родительского класса
     * @return regex паттерн или null
     */
    public static String resolveStringPattern(MemberScope<?, ?> member,
                                              Function<MemberScope<?, ?>, String> superResolver) {
        if (member.getType().isInstanceOf(UUID.class)) {
            return "^%s$".formatted(Regex.getRegexOrDefault(UUID.class.getName()));
        }

        return Optional
                .ofNullable(superResolver.apply(member))
                .or(() -> getSchemaAnnotationValue(member, Schema::pattern, pattern -> !pattern.isEmpty()))
                .orElse(null);
    }
} 