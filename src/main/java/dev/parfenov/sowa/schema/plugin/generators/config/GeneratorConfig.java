package dev.parfenov.sowa.schema.plugin.generators.config;

import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;

/**
 * Конфигурация для генератора JSON Schema.
 * <p>
 * Настраивает параметры генерации схем включая опции валидации,
 * обработку определений и интеграцию с Jakarta Validation.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
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

        var configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON);

        //Опции библиотеки
        configBuilder
                .with(Option.SCHEMA_VERSION_INDICATOR)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                .with(Option.NULLABLE_FIELDS_BY_DEFAULT)
                .with(Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .with(Option.INLINE_NULLABLE_SCHEMAS)
                .with(Option.ENUM_KEYWORD_FOR_SINGLE_VALUES)
                .with(Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                .with(Option.ALLOF_CLEANUP_AT_THE_END);

        //Модули
        configBuilder.with(new CustomJakartaValidationModule(stringLengthIncreasePercent, JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS));
        configBuilder.with(new CustomJacksonModule(configBuilder.getObjectMapper()));

        //Главная схема
        configBuilder.forTypesInGeneral().withCustomDefinitionProvider(new CustomObjectDefinitionProvider());
        configBuilder.forTypesInGeneral().withCustomDefinitionProvider(new CustomVoidDefinitionProvider());
        configBuilder.forTypesInGeneral().withTypeAttributeOverride(new CustomTypeAttributeOverride());
        configBuilder.forTypesInGeneral().withTypeAttributeOverride(new CustomTypeMapAttributeOverride());
        configBuilder.forTypesInGeneral().withTypeAttributeOverride(new AllOfAttributeOverride());

        //Поля
        configBuilder.forFields().withCustomDefinitionProvider(new CustomFieldMapDefinitionProvider());

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
