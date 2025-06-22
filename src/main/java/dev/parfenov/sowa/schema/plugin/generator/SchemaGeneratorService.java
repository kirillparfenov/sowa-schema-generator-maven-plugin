package dev.parfenov.sowa.schema.plugin.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import dev.parfenov.sowa.schema.plugin.config.CustomJakartaValidationModule;

import java.util.Set;

public class SchemaGeneratorService {

    private final ObjectMapper mapper;
    private final SchemaGenerator INSTANCE;

    public SchemaGeneratorService() {
        mapper = new ObjectMapper();
        INSTANCE = new SchemaGenerator(
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                        .with(new JacksonModule())
                        .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                        .with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                        .with(Option.NULLABLE_FIELDS_BY_DEFAULT)
                        .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                        .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                        .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                        .with(new CustomJakartaValidationModule())
                        .build()
        );
    }

    public void generate(Set<Class<?>> classes) {
        classes.forEach(c -> {
            var schema = generateSchema(c);
            try {
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ObjectNode generateGenericSchema(Class<?> page, Class<?> element) {
        return INSTANCE.generateSchema(page, element);
    }

    public ObjectNode generateSchema(Class<?> element) {
        return INSTANCE.generateSchema(element);
    }

}
