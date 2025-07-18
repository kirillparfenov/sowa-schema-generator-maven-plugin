/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.generator;

import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static dev.parfenov.sowa.schema.plugin.generator.ConstraintResolver.resolveNumericMaximum;
import static dev.parfenov.sowa.schema.plugin.generator.MemberAnnotationExtractor.getSchemaAnnotationValue;
import static dev.parfenov.sowa.schema.plugin.generator.ValidationConstants.ARRAY_MAX_SIZE;

/**
 * Кастомный модуль валидации Jakarta для генерации JSON Schema.
 * <p>
 * Расширяет стандартный JakartaValidationModule дополнительной поддержкой
 * аннотаций Swagger/OpenAPI и настраиваемыми ограничениями валидации.
 * <p>
 * Основные возможности:
 * <ul>
 *   <li>Поддержка аннотаций @Schema и @ArraySchema</li>
 *   <li>Настраиваемое увеличение длины строк</li>
 *   <li>Автоматические ограничения для типов (UUID, числа)</li>
 *   <li>Определение максимальных размеров массивов</li>
 * </ul>
 */
public class CustomJakartaValidationModule extends JakartaValidationModule {

    private final StringLengthCalculator lengthCalculator;

    /**
     * Создает модуль с настраиваемым процентом увеличения длины строк.
     *
     * @param stringLengthIncreasePercent процент увеличения длины строк (0-100)
     * @param options                     дополнительные опции Jakarta валидации
     */
    public CustomJakartaValidationModule(int stringLengthIncreasePercent, JakartaValidationOption... options) {
        super(options);
        this.lengthCalculator = new StringLengthCalculator(stringLengthIncreasePercent);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        super.applyToConfigBuilder(builder);
        applyToConfigBuilder(builder.forTypesInGeneral());
        applyToConfigBuilder(builder.forFields());
        applyToConfigBuilder(builder.forMethods());
    }

    /**
     * Применяет настройки к общей конфигурации типов.
     */
    private void applyToConfigBuilder(SchemaGeneratorGeneralConfigPart configPart) {
        configPart.withArrayMaxItemsResolver(this::resolveArrayMaxItems);
        configPart.withNumberInclusiveMaximumResolver(this::resolveTypeMaximum);
        configPart.withCustomDefinitionProvider(new CustomObjectDefinitionProvider());
        configPart.withDescriptionResolver(this.createTypePropertyResolver(Schema::description, description -> !description.isBlank()));
        configPart.withTypeAttributeOverride(new CustomTypeAttributeOverride());
    }

    /**
     * Применяет настройки к общей части конфигурации.
     */
    private void applyToConfigBuilder(SchemaGeneratorConfigPart<?> configPart) {
        configPart.withDescriptionResolver(this::resolveDescription);
    }

    /**
     * Разрешает описание из аннотации Schema.
     */
    private String resolveDescription(MemberScope<?, ?> member) {
        return getSchemaAnnotationValue(
                member,
                Schema::description,
                description -> !description.isEmpty()
        ).orElse(null);
    }

    /**
     * Разрешает максимальный размер массива для типа.
     */
    private Integer resolveArrayMaxItems(TypeScope typeScope) {
        return typeScope.isContainerType() ? ARRAY_MAX_SIZE : null;
    }

    /**
     * Разрешает максимальное числовое значение для типа.
     */
    private BigDecimal resolveTypeMaximum(TypeScope typeScope) {
        return resolveNumericMaximum(typeScope.getType());
    }

    @Override
    protected Boolean isNullable(MemberScope<?, ?> member) {
        return super.isNullable(member);
//        return Optional
//                .ofNullable(super.isNullable(member))
//                .orElseGet(() -> checkNullable(member)); //вернуть, если потребуется
    }

    /**
     * Проверяет nullable свойство из аннотации Schema.
     */
    private Boolean checkNullable(MemberScope<?, ?> member) {
        return getSchemaAnnotationValue(member, Function.identity(), x -> true)
                .map(Schema::nullable)
                .orElse(null);
    }

    @Override
    protected Integer resolveStringMaxLength(MemberScope<?, ?> member) {
        return ConstraintResolver.resolveStringMaxLength(member, lengthCalculator, super::resolveStringMaxLength);
    }

    @Override
    protected String resolveStringPattern(MemberScope<?, ?> member) {
        return ConstraintResolver.resolveStringPattern(member, super::resolveStringPattern);
    }

    @Override
    protected BigDecimal resolveNumberInclusiveMaximum(MemberScope<?, ?> member) {
        return Optional
                .ofNullable(super.resolveNumberInclusiveMaximum(member))
                .orElseGet(() -> resolveInclusiveMaximum(member));
    }

    /**
     * Разрешает включающий максимум из аннотации Schema.
     */
    private BigDecimal resolveInclusiveMaximum(MemberScope<?, ?> memberScope) {
        return getSchemaAnnotationValue(memberScope, Schema::maximum, maximum -> !maximum.isEmpty())
                .filter(maximum -> getSchemaAnnotationValue(memberScope, Schema::exclusiveMaximum, Boolean.FALSE::equals).isPresent())
                .map(BigDecimal::new)
                .orElseGet(() -> resolveNumericMaximum(memberScope.getType()));
    }

    @Override
    protected Integer resolveArrayMaxItems(MemberScope<?, ?> member) {
        if (!member.isContainerType()) {
            return null;
        }

        var maxItems = super.resolveArrayMaxItems(member);
        return Objects.isNull(maxItems) ? ARRAY_MAX_SIZE : maxItems;
    }

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
     * Создает resolver для свойств типа из аннотации Schema.
     *
     * @param <T>            тип извлекаемого значения
     * @param valueExtractor функция извлечения значения
     * @param valueFilter    предикат фильтрации
     * @return ConfigFunction для разрешения свойства
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
