package dev.parfenov.sowa.schema.plugin.generator;

import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;

public class GeneratorConfig {

    private final SchemaGeneratorConfig config;

    public GeneratorConfig() {
        var configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                .with(new JacksonModule())
                .with(Option.SCHEMA_VERSION_INDICATOR)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                .with(Option.NULLABLE_FIELDS_BY_DEFAULT)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .with(Option.DEFINITION_FOR_MAIN_SCHEMA)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                /// если не передать JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS - не будет учитываться аннотация @Pattern
                /// и даже не передастся String resolveStringPattern(MemberScope<?, ?> member) в  SchemaGeneratorConfigPart<?>.withStringPatternResolver
                .with(new CustomJakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS));
        this.config = configBuilder.build();
    }

    public SchemaGeneratorConfig getConfig() {
        return config;
    }
}
