package dev.parfenov.sowa.schema.plugin.generator;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class CustomJakartaValidationModule extends JakartaValidationModule {

    private static final Integer ARRAY_MAX_SIZE = 1000;
    private final int stringLengthIncreasePercent;

    public CustomJakartaValidationModule(int stringLengthIncreasePercent, JakartaValidationOption... options) {
        super(options);
        this.stringLengthIncreasePercent = stringLengthIncreasePercent;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        super.applyToConfigBuilder(builder);
        this.applyToConfigBuilder(builder.forTypesInGeneral());
        this.applyToConfigBuilder(builder.forFields());
        this.applyToConfigBuilder(builder.forMethods());

    }

    private void applyToConfigBuilder(SchemaGeneratorConfigPart<?> configPart) {
        configPart.withTargetTypeOverridesResolver(this::resolveTargetTypeOverrides);
        configPart.withDescriptionResolver(this::resolveDescription);
    }

    private String resolveDescription(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(
                member,
                Schema::description,
                description -> !description.isEmpty()
        ).orElse(null);
    }

    private List<ResolvedType> resolveTargetTypeOverrides(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(
                        member,
                        Schema::implementation,
                        annotatedImplementation -> annotatedImplementation != Void.class)
                .map(annotatedType -> member.getContext().resolve(annotatedType))
                .map(Collections::singletonList)
                .orElse(null);
    }

    @Override
    protected Boolean isNullable(MemberScope<?, ?> member) {
        return Optional
                .ofNullable(super.isNullable(member))
                .orElseGet(() -> this.checkNullable(member));
    }

    private Boolean checkNullable(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Function.identity(), x -> true)
                .map(Schema::nullable)
                .orElse(null);
    }

    private void applyToConfigBuilder(SchemaGeneratorGeneralConfigPart configPart) {
        configPart.withArrayMaxItemsResolver(typeScope -> {
            if (typeScope.isContainerType()) {
                return ARRAY_MAX_SIZE;
            }
            return null;
        });

        configPart.withNumberInclusiveMaximumResolver(typeScope -> this.inclusiveMaximum(typeScope.getType()));
        configPart.withDescriptionResolver(this.createTypePropertyResolver(Schema::description, description -> !description.isBlank()));
    }

    @Override
    protected Integer resolveStringMaxLength(MemberScope<?, ?> member) {
        var maxStringLength = super.resolveStringMaxLength(member);
        if (maxStringLength != null) return increaseLength(maxStringLength);

        maxStringLength = getSchemaAnnotationValue(
                member,
                Schema::maxLength,
                maxLength -> maxLength < Integer.MAX_VALUE && maxLength > -1
        ).orElse(null);
        if (maxStringLength != null) return increaseLength(maxStringLength);

        if (member.getType().isInstanceOf(CharSequence.class)) {
            return 300;
        }

        if (member.getType().isInstanceOf(UUID.class)) {
            return 36;
        }
        return null;
    }

    private Integer increaseLength(Integer stringLength) {
        if (stringLengthIncreasePercent > 0) {
            var result = stringLength * (1 + stringLengthIncreasePercent / 100.0);
            return (int) (Math.round(result / 100.0) * 100.0);
        }
        return stringLength;
    }

    private <T> Optional<T> getSchemaAnnotationValue(MemberScope<?, ?> member,
                                                     Function<Schema, T> valueExtractor,
                                                     Predicate<T> valueFilter) {
        if (member.isFakeContainerItemScope()) {
            return this.getArraySchemaAnnotation(member)
                    .map(ArraySchema::schema)
                    .map(valueExtractor)
                    .filter(valueFilter);
        }
        var annotation = member.getAnnotationConsideringFieldAndGetter(Schema.class);
        if (annotation != null) {
            return Optional.of(annotation)
                    .map(valueExtractor)
                    .filter(valueFilter);
        }
        return this.getArraySchemaAnnotation(member)
                .map(ArraySchema::arraySchema)
                .map(valueExtractor)
                .filter(valueFilter);
    }

    private Optional<ArraySchema> getArraySchemaAnnotation(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetter(ArraySchema.class));
    }

    @Override
    protected String resolveStringPattern(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(UUID.class)) {
            return "^%s$".formatted(Regex.getRegexOrDefault(UUID.class.getName()));
        }
        return Optional
                .ofNullable(super.resolveStringPattern(member))
                .or(() -> this.getSchemaAnnotationValue(member, Schema::pattern, pattern -> !pattern.isEmpty()))
                .orElse(null);
    }

    @Override
    protected BigDecimal resolveNumberInclusiveMaximum(MemberScope<?, ?> member) {
        return Optional
                .ofNullable(super.resolveNumberInclusiveMaximum(member))
                .orElseGet(() -> this.inclusiveMaximum(member));
    }

    private BigDecimal inclusiveMaximum(MemberScope<?, ?> memberScope) {
        return this.getSchemaAnnotationValue(memberScope, Schema::maximum, maximum -> !maximum.isEmpty())
                .filter(maximum -> this.getSchemaAnnotationValue(memberScope, Schema::exclusiveMaximum, Boolean.FALSE::equals).isPresent())
                .map(BigDecimal::new)
                .orElseGet(() -> inclusiveMaximum(memberScope.getType()));
    }

    private BigDecimal inclusiveMaximum(ResolvedType typeScope) {
        if (typeScope.isInstanceOf(Integer.class) || typeScope.isInstanceOf(int.class)) {
            return new BigDecimal(Integer.MAX_VALUE);
        } else if (typeScope.isInstanceOf(Long.class) || typeScope.isInstanceOf(long.class)) {
            return new BigDecimal(Long.MAX_VALUE);
        } else if (typeScope.isInstanceOf(Byte.class) || typeScope.isInstanceOf(byte.class)) {
            return new BigDecimal(Byte.MAX_VALUE);
        }

        return null;
    }

    @Override
    protected Integer resolveArrayMaxItems(MemberScope<?, ?> member) {
        if (member.isContainerType()) {
            var maxItems = super.resolveArrayMaxItems(member);
            return Objects.isNull(maxItems) ? ARRAY_MAX_SIZE : maxItems;
        }
        return null;
    }

    @Override
    protected Integer resolveStringMinLength(MemberScope<?, ?> member) {
        var stringMinLength = super.resolveStringMinLength(member);
        if (stringMinLength != null) return stringMinLength;

        return this.getSchemaAnnotationValue(member, Schema::minLength, minLength -> minLength > 0)
                .orElse(null);
    }

    private <T> ConfigFunction<TypeScope, T> createTypePropertyResolver(
            Function<Schema, T> valueExtractor,
            Predicate<T> valueFilter
    ) {
        return typeScope -> Optional
                .ofNullable(typeScope.getType().getErasedType().getAnnotation(Schema.class))
                .map(valueExtractor)
                .filter(valueFilter)
                .orElse(null);
    }
}
