package dev.parfenov.sowa.schema.plugin.generators.config;

import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import dev.parfenov.sowa.schema.plugin.parsers.TypesParser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static dev.parfenov.sowa.schema.plugin.generators.config.ConstraintResolver.resolveNumericMaximum;
import static dev.parfenov.sowa.schema.plugin.generators.config.ConstraintResolver.resolveNumericMinimum;
import static dev.parfenov.sowa.schema.plugin.generators.config.MemberAnnotationExtractor.getSchemaAnnotationValue;
import static dev.parfenov.sowa.schema.plugin.generators.config.ValidationConstants.ARRAY_MAX_SIZE;

/**
 * Кастомный модуль валидации Jakarta для генерации JSON Schema.
 * <p>
 * Расширяет стандартный {@link JakartaValidationModule} дополнительной поддержкой
 * аннотаций Swagger/OpenAPI и настраиваемыми ограничениями валидации.
 * <p>
 * Основные возможности:
 * <ul>
 *   <li>Поддержка аннотаций {@link Schema @Schema} из OpenAPI</li>
 *   <li>Настраиваемое увеличение длины строк через {@link StringLengthCalculator}</li>
 *   <li>Автоматические ограничения для примитивных типов (int, long, etc.)</li>
 *   <li>Автоматические ограничения для специальных типов (UUID)</li>
 *   <li>Определение максимальных размеров массивов</li>
 *   <li>Интеграция с {@link ConstraintResolver} для единообразного разрешения ограничений</li>
 * </ul>
 * <p>
 * Пример использования:
 * <pre>{@code
 * var module = new CustomJakartaValidationModule(10, JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED);
 * var config = SchemaGeneratorConfigBuilder.forObjectMapper(objectMapper)
 *     .with(module)
 *     .build();
 * }</pre>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025
 * @see JakartaValidationModule Базовый модуль Jakarta валидации
 * @see ConstraintResolver Утилиты для разрешения ограничений
 * @see StringLengthCalculator Калькулятор длины строк
 */
public class CustomJakartaValidationModule extends JakartaValidationModule {

    private final StringLengthCalculator lengthCalculator;

    /**
     * Создает модуль с настраиваемым процентом увеличения длины строк.
     * <p>
     * Процент увеличения применяется ко всем строковым ограничениям длины
     * для создания более гибких схем валидации.
     *
     * @param stringLengthIncreasePercent процент увеличения длины строк (0-100),
     *                                    где 0 означает отсутствие увеличения,
     *                                    а 10 означает увеличение на 10%
     * @param options                     дополнительные опции Jakarta валидации
     *                                    (например, {@link JakartaValidationOption#NOT_NULLABLE_FIELD_IS_REQUIRED})
     * @see StringLengthCalculator Логика увеличения длины
     */
    public CustomJakartaValidationModule(int stringLengthIncreasePercent, JakartaValidationOption... options) {
        super(options);
        this.lengthCalculator = new StringLengthCalculator(stringLengthIncreasePercent);
    }

    /**
     * Применяет конфигурацию модуля к билдеру схемы.
     * <p>
     * Настраивает все аспекты генерации: типы, поля, методы и их ограничения.
     *
     * @param builder билдер конфигурации генератора схем, не null
     */
    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        super.applyToConfigBuilder(builder);
        applyToConfigBuilder(builder.forTypesInGeneral());
        applyToConfigBuilder(builder.forFields());
        applyToConfigBuilder(builder.forMethods());
    }

    /**
     * Применяет настройки к общей конфигурации типов.
     * <p>
     * Конфигурирует:
     * <ul>
     *   <li>Максимальные размеры массивов</li>
     *   <li>Числовые ограничения максимума</li>
     *   <li>Кастомные провайдеры определений</li>
     *   <li>Описания типов из {@link Schema @Schema}</li>
     *   <li>Кастомные атрибуты типов</li>
     * </ul>
     *
     * @param configPart часть конфигурации для общих настроек типов
     */
    private void applyToConfigBuilder(SchemaGeneratorGeneralConfigPart configPart) {
        configPart.withArrayMaxItemsResolver(this::resolveArrayMaxItems);
//        configPart.withNumberInclusiveMaximumResolver(this::resolveTypeMaximum);
        configPart.withDescriptionResolver(this.createTypePropertyResolver(Schema::description, description -> !description.isBlank()));
    }

    /**
     * Применяет настройки к общей части конфигурации (поля и методы).
     * <p>
     * Настраивает описания для полей и методов из аннотаций {@link Schema @Schema}.
     *
     * @param configPart часть конфигурации для полей или методов
     */
    private void applyToConfigBuilder(SchemaGeneratorConfigPart<?> configPart) {
        configPart.withDescriptionResolver(this::resolveDescription);
    }

    /**
     * Извлекает описание из аннотации {@link Schema @Schema}.
     *
     * @param member область видимости члена класса (поле/метод), не null
     * @return описание из аннотации или null если не найдено
     */
    private String resolveDescription(MemberScope<?, ?> member) {
        return getSchemaAnnotationValue(
                member,
                Schema::description,
                description -> !description.isEmpty()
        ).orElse(null);
    }

    /**
     * Определяет максимальный размер массива для типа.
     * <p>
     * Применяется только к контейнерным типам (массивы, коллекции).
     * Возвращает {@value ValidationConstants#ARRAY_MAX_SIZE} для всех массивов.
     *
     * @param typeScope область видимости типа, не null
     * @return {@value ValidationConstants#ARRAY_MAX_SIZE} для массивов, null для других типов
     */
    private Integer resolveArrayMaxItems(TypeScope typeScope) {
        return typeScope.isContainerType() ? ARRAY_MAX_SIZE : null;
    }

//    /**
//     * Определяет максимальное числовое значение для типа.
//     * <p>
//     * Использует {@link ConstraintResolver#resolveNumericMaximum(com.fasterxml.classmate.ResolvedType)}
//     * для получения максимального значения примитивных числовых типов.
//     *
//     * @param typeScope область видимости типа, не null
//     * @return максимальное значение типа или null если не поддерживается
//     * @see ConstraintResolver#resolveNumericMaximum
//     */
//    private BigDecimal resolveTypeMaximum(TypeScope typeScope) {
//        return resolveNumericMaximum(typeScope.getType());
//    }

    /**
     * Определяет nullable свойство для члена класса.
     * <p>
     * Логика разрешения:
     * <ol>
     *   <li>Проверяет базовую Jakarta валидацию (родительский класс)</li>
     *   <li>Для примитивных типов возвращает true (nullable)</li>
     * </ol>
     * <p>
     * Примечание: примитивы помечаются как nullable для совместимости
     * с генерацией схемы, где они могут быть представлены как Optional.
     *
     * @param member область видимости члена класса, не null
     * @return true если поле может быть null, false если не может, null если не определено
     */
    @Override
    protected Boolean isNullable(MemberScope<?, ?> member) {
        return Optional
                .ofNullable(super.isNullable(member))
                .orElseGet(() -> TypesParser.isPrimitive(member) ? true : null);
    }

    /**
     * Разрешает максимальную длину строки с учетом увеличения.
     * <p>
     * Делегирует логику {@link ConstraintResolver#resolveStringMaxLength}
     * с использованием настроенного {@link StringLengthCalculator}.
     *
     * @param member область видимости члена класса, не null
     * @return максимальная длина строки с учетом увеличения, или null
     * @see ConstraintResolver#resolveStringMaxLength
     */
    @Override
    protected Integer resolveStringMaxLength(MemberScope<?, ?> member) {
        return ConstraintResolver.resolveStringMaxLength(member, lengthCalculator, super::resolveStringMaxLength);
    }

    /**
     * Разрешает regex паттерн для строки.
     * <p>
     * Делегирует логику {@link ConstraintResolver#resolveStringPattern}
     * для единообразного разрешения паттернов.
     *
     * @param member область видимости члена класса, не null
     * @return regex паттерн или null если не найден
     * @see ConstraintResolver#resolveStringPattern
     */
    @Override
    protected String resolveStringPattern(MemberScope<?, ?> member) {
        return ConstraintResolver.resolveStringPattern(member, super::resolveStringPattern);
    }

    /**
     * Разрешает включающий максимум для числового поля.
     * <p>
     * Сначала проверяет Jakarta валидацию, затем аннотации {@link Schema @Schema}.
     *
     * @param member область видимости члена класса, не null
     * @return включающий максимум или null если не определен
     */
    @Override
    protected BigDecimal resolveNumberInclusiveMaximum(MemberScope<?, ?> member) {
        return Optional
                .ofNullable(super.resolveNumberInclusiveMaximum(member))
                .orElseGet(() -> resolveInclusiveMaximum(member));
    }

    /**
     * Разрешает включающий минимум для числового поля.
     * <p>
     * Сначала проверяет Jakarta валидацию, затем аннотации {@link Schema @Schema}.
     *
     * @param member область видимости члена класса, не null
     * @return включающий минимум или null если не определен
     */
    @Override
    protected BigDecimal resolveNumberInclusiveMinimum(MemberScope<?, ?> member) {
        return Optional
                .ofNullable(super.resolveNumberInclusiveMinimum(member))
                .orElseGet(() -> resolveInclusiveMinimum(member));
    }

    /**
     * Извлекает включающий максимум из аннотации {@link Schema @Schema}.
     * <p>
     * Проверяет {@link Schema#maximum()} и учитывает {@link Schema#exclusiveMaximum()}.
     * Если максимум не исключающий, возвращает значение. В противном случае
     * использует автоматические ограничения типа.
     *
     * @param memberScope область видимости члена класса
     * @return включающий максимум из Schema или автоматический максимум типа
     */
    private BigDecimal resolveInclusiveMaximum(MemberScope<?, ?> memberScope) {
        return getSchemaAnnotationValue(memberScope, Schema::maximum, maximum -> !maximum.isEmpty())
                .filter(maximum -> getSchemaAnnotationValue(memberScope, Schema::exclusiveMaximum, Boolean.FALSE::equals).isPresent())
                .map(BigDecimal::new)
                .orElseGet(() -> resolveNumericMaximum(memberScope.getType()));
    }

    /**
     * Извлекает включающий минимум из аннотации {@link Schema @Schema}.
     * <p>
     * Проверяет {@link Schema#minimum()} и учитывает {@link Schema#exclusiveMinimum()}.
     * Если минимум не исключающий, возвращает значение. В противном случае
     * использует автоматические ограничения типа.
     *
     * @param memberScope область видимости члена класса
     * @return включающий минимум из Schema или автоматический минимум типа
     */
    private BigDecimal resolveInclusiveMinimum(MemberScope<?, ?> memberScope) {
        return getSchemaAnnotationValue(memberScope, Schema::minimum, minimum -> !minimum.isEmpty())
                .filter(minimum -> getSchemaAnnotationValue(memberScope, Schema::exclusiveMinimum, Boolean.FALSE::equals).isPresent())
                .map(BigDecimal::new)
                .orElseGet(() -> resolveNumericMinimum(memberScope.getType()));
    }

    /**
     * Разрешает максимальное количество элементов в массиве.
     * <p>
     * Сначала проверяет Jakarta валидацию, затем применяет значение по умолчанию
     * {@value ValidationConstants#ARRAY_MAX_SIZE} для всех массивов.
     *
     * @param member область видимости члена класса, не null
     * @return максимальное количество элементов или null для не-массивов
     */
    @Override
    protected Integer resolveArrayMaxItems(MemberScope<?, ?> member) {
        if (!member.isContainerType()) {
            return null;
        }

        var maxItems = super.resolveArrayMaxItems(member);
        return Objects.isNull(maxItems) ? ARRAY_MAX_SIZE : maxItems;
    }

    /**
     * Разрешает минимальную длину строки.
     * <p>
     * Сначала проверяет Jakarta валидацию, затем аннотацию {@link Schema#minLength()}.
     *
     * @param member область видимости члена класса, не null
     * @return минимальная длина строки или null если не определена
     */
    @Override
    protected Integer resolveStringMinLength(MemberScope<?, ?> member) {
        var stringMinLength = super.resolveStringMinLength(member);
        if (stringMinLength != null) {
            return stringMinLength;
        }

        return getSchemaAnnotationValue(member, Schema::minLength, minLength -> minLength > 0)
                .orElse(null);
    }

    /**
     * Создает resolver для извлечения свойств типа из аннотации {@link Schema @Schema}.
     * <p>
     * Утилитарный метод для создания функций, которые извлекают определенные
     * свойства из аннотации Schema на уровне типа (класса).
     *
     * @param <T>            тип извлекаемого значения
     * @param valueExtractor функция для извлечения значения из Schema, не null
     * @param valueFilter    предикат для фильтрации извлеченных значений, не null
     * @return ConfigFunction для использования в конфигурации генератора
     */
    private <T> ConfigFunction<TypeScope, T> createTypePropertyResolver(
            Function<Schema, T> valueExtractor,
            Predicate<T> valueFilter) {
        return typeScope -> Optional
                .ofNullable(typeScope.getType().getErasedType().getAnnotation(Schema.class))
                .map(valueExtractor)
                .filter(valueFilter)
                .orElse(null);
    }
}
