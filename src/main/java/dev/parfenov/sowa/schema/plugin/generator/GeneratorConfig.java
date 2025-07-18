/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.generator;

import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;

/**
 * Конфигурация для генератора JSON Schema.
 * <p>
 * Настраивает параметры генерации схем включая опции валидации,
 * обработку определений и интеграцию с Jakarta Validation.
 */
public class GeneratorConfig {

    private final SchemaGeneratorConfig config;
    private final boolean extractDefinitions;

    /**
     * Создает конфигурацию генератора.
     *
     * @param extractDefinitions          флаг для извлечения определений в отдельные объекты
     * @param stringLengthIncreasePercent процент увеличения длины строк для валидации
     */
    public GeneratorConfig(boolean extractDefinitions, int stringLengthIncreasePercent) {
        this.extractDefinitions = extractDefinitions;

        var configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                .with(new JacksonModule())
                .with(Option.SCHEMA_VERSION_INDICATOR)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                .with(Option.NULLABLE_FIELDS_BY_DEFAULT)
                .with(Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .with(Option.INLINE_NULLABLE_SCHEMAS)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                .with(Option.ENUM_KEYWORD_FOR_SINGLE_VALUES)
                /// если не передать JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS - не будет учитываться аннотация @Pattern
                /// и даже не передастся String resolveStringPattern(MemberScope<?, ?> member) в  SchemaGeneratorConfigPart<?>.withStringPatternResolver
                .with(new CustomJakartaValidationModule(stringLengthIncreasePercent, JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS));

        if (extractDefinitions) {
            configBuilder.with(Option.DEFINITION_FOR_MAIN_SCHEMA);
        }
        this.config = configBuilder.build();
    }

    /**
     * Возвращает конфигурацию генератора схем.
     *
     * @return настроенная конфигурация SchemaGeneratorConfig
     */
    public SchemaGeneratorConfig getConfig() {
        return config;
    }

    /**
     * Проверяет, включено ли извлечение определений.
     *
     * @return true если определения должны извлекаться отдельно
     */
    public boolean isExtractDefinitions() {
        return extractDefinitions;
    }
}
