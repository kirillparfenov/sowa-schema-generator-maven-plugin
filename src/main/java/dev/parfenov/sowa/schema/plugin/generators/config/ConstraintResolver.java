package dev.parfenov.sowa.schema.plugin.generators.config;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.MemberScope;
import dev.parfenov.sowa.schema.plugin.generators.PathRegexResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.ReflectionUtils;

import java.lang.invoke.MethodType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static dev.parfenov.sowa.schema.plugin.generators.config.MemberAnnotationExtractor.getSchemaAnnotationValue;
import static dev.parfenov.sowa.schema.plugin.generators.config.ValidationConstants.DEFAULT_STRING_MAX_LENGTH;
import static dev.parfenov.sowa.schema.plugin.generators.config.ValidationConstants.UUID_MAX_LENGTH;

/**
 * Утилитарный класс для определения ограничений типов в JSON Schema.
 * <p>
 * Предоставляет методы для определения ограничений типов на основе:
 * <ul>
 *     <li>Аннотаций {@link Schema} из OpenAPI</li>
 *     <li>Jakarta Validation аннотаций</li>
 *     <li>Характеристик самих типов (примитивы, UUID и т.д.)</li>
 * </ul>
 * <p>
 * Все методы статические и null-safe. Класс нельзя инстанцировать.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @see Schema OpenAPI Schema аннотация
 * @see ValidationConstants Константы для валидации
 * @since 2025-08-03
 */
public final class ConstraintResolver {

    private ConstraintResolver() {
        // Утилитарный класс - запрещаем создание экземпляров
    }

    /**
     * Определяет максимальную длину строки для поля.
     * <p>
     * Логика разрешения (в порядке приоритета):
     * <ol>
     *     <li>Jakarta Validation аннотации (через superResolver)</li>
     *     <li>Аннотация {@link Schema#maxLength()}</li>
     *     <li>Длина по умолчанию для типа</li>
     * </ol>
     * <p>
     * Примеры:
     * <pre>{@code
     * // Для UUID -> 36
     * // Для String с @Schema(maxLength = 50) -> 55 (с учетом увеличения)
     * // Для обычного String -> 330 (с учетом увеличения от 300)
     * }</pre>
     *
     * @param member           область видимости члена класса (поле/геттер), не null
     * @param lengthCalculator калькулятор для увеличения длины строк, не null
     * @param superResolver    базовый resolver Jakarta Validation аннотаций, не null
     * @return максимальная длина строки с учетом увеличения, или null если ограничение не найдено
     */
    public static Integer resolveStringMaxLength(MemberScope<?, ?> member,
                                                 StringLengthCalculator lengthCalculator,
                                                 Function<MemberScope<?, ?>, Integer> superResolver) {
        // Сначала проверяем базовую валидацию (например, @Size, @Length)
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
        return getDefaultLengthForType(member, lengthCalculator);
    }

    /**
     * Получает длину по умолчанию для конкретного типа.
     * <p>
     * Поддерживаемые типы:
     * <ul>
     *     <li>{@link CharSequence} (String, StringBuilder и т.д.) -> {@value ValidationConstants#DEFAULT_STRING_MAX_LENGTH}</li>
     *     <li>{@link UUID} -> {@value ValidationConstants#UUID_MAX_LENGTH}</li>
     * </ul>
     *
     * @param member область видимости члена класса, не null
     * @return длина по умолчанию для типа, или null если тип не поддерживается
     */
    private static Integer getDefaultLengthForType(MemberScope<?, ?> member, StringLengthCalculator lengthCalculator) {
        if (member.getType().isInstanceOf(CharSequence.class)) {
            return lengthCalculator.increaseLength(DEFAULT_STRING_MAX_LENGTH);
        }

        if (member.getType().isInstanceOf(UUID.class)) {
            return UUID_MAX_LENGTH;
        }

        return null;
    }

    /**
     * Определяет максимальное значение для числовых типов.
     * <p>
     * Поддерживаемые типы: все примитивные числовые типы и их обертки
     * (byte, short, int, long, float, double), исключая BigDecimal, BigInteger и char.
     * <p>
     * Примеры:
     * <pre>{@code
     * // Для Integer -> 2147483647
     * // Для Long -> 9223372036854775807
     * // Для BigDecimal -> null (не поддерживается)
     * }</pre>
     *
     * @param type разрешенный тип для анализа, не null
     * @return максимальное значение типа в виде BigDecimal, или null если тип не поддерживается
     */
    public static BigDecimal resolveNumericMaximum(ResolvedType type) {
        return getNumberClass(type)
                .map(clazz -> getFieldValue(clazz, "MAX_VALUE"))
                .map(Objects::toString)
                .map(BigDecimal::new)
                .orElse(null);
    }

    /**
     * Определяет минимальное значение для числовых типов.
     * <p>
     * Поддерживаемые типы: все примитивные числовые типы и их обертки
     * (byte, short, int, long, float, double), исключая BigDecimal, BigInteger и char.
     * <p>
     * Примеры:
     * <pre>{@code
     * // Для Integer -> -2147483648
     * // Для Long -> -9223372036854775808
     * // Для BigDecimal -> null (не поддерживается)
     * }</pre>
     *
     * @param type разрешенный тип для анализа, не null
     * @return минимальное значение типа в виде BigDecimal, или null если тип не поддерживается
     */
    public static BigDecimal resolveNumericMinimum(ResolvedType type) {
        return getNumberClass(type)
                .map(clazz -> getFieldValue(clazz, "MIN_VALUE"))
                .map(Objects::toString)
                .map(BigDecimal::new)
                .orElse(null);
    }

    /**
     * Получает класс числового типа для дальнейшего анализа.
     * <p>
     * Исключения:
     * <ul>
     *     <li>BigDecimal и BigInteger - не имеют фиксированных границ</li>
     *     <li>Character и char - не являются числовыми в контексте схем</li>
     * </ul>
     *
     * @param type разрешенный тип для анализа
     * @return Optional с классом числового типа или empty если тип не поддерживается
     */
    public static Optional<Class<?>> getNumberClass(ResolvedType type) {
        if (
                type.isInstanceOf(BigDecimal.class) || type.isInstanceOf(BigInteger.class)
                        || type.isInstanceOf(Character.class) || type.isInstanceOf(char.class)
        ) {
            return Optional.empty();
        }

        if (type.isPrimitive()) {
            return Optional.ofNullable(
                    MethodType.methodType(type.getErasedType())
                            .wrap()
                            .returnType()
            );
        }

        if (type.isInstanceOf(Number.class)) {
            return Optional.ofNullable(type.getErasedType());
        }

        return Optional.empty();
    }

    /**
     * Извлекает значение статического поля из класса с помощью рефлексии.
     *
     * @param clazz     класс для поиска поля
     * @param fieldName имя статического поля (например, "MIN_VALUE", "MAX_VALUE")
     * @return значение поля как Number, или null если поле не найдено
     */
    private static Number getFieldValue(Class<?> clazz, String fieldName) {
        var fieldValue = ReflectionUtils.findField(clazz, fieldName);
        if (fieldValue == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(fieldValue);

        return (Number) ReflectionUtils.getField(fieldValue, null);
    }

    /**
     * Определяет regex паттерн для строковых типов.
     * <p>
     * Логика разрешения (в порядке приоритета):
     * <ol>
     *     <li>UUID тип -> стандартный UUID regex</li>
     *     <li>Jakarta Validation аннотации (через superResolver)</li>
     *     <li>Аннотация {@link Schema#pattern()}</li>
     * </ol>
     * <p>
     * Примеры:
     * <pre>{@code
     * // Для UUID -> "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
     * // Для @Pattern(regexp = "\\d+") -> "\\d+"
     * // Для @Schema(pattern = "[A-Z]+") -> "[A-Z]+"
     * }</pre>
     *
     * @param member        область видимости члена класса, не null
     * @param superResolver базовый resolver Jakarta Validation аннотаций, не null
     * @return regex паттерн в виде строки, или null если паттерн не найден
     */
    public static String resolveStringPattern(MemberScope<?, ?> member,
                                              Function<MemberScope<?, ?>, String> superResolver) {
        return Optional
                .ofNullable(superResolver.apply(member))
                .or(() -> getSchemaAnnotationValue(member, Schema::pattern, pattern -> !pattern.isEmpty()))
                .orElseGet(() -> resolveStringPatternByType(member.getType().getErasedType()));
    }

    private static String resolveStringPatternByType(Class<?> clazz) {
        return UUID.class.isAssignableFrom(clazz)
                ? "^%s$".formatted(PathRegexResolver.uidRegex())
                : null;
    }
} 