package dev.parfenov.sowa.schema.plugin.generator;

import com.github.victools.jsonschema.generator.SchemaGenerator;

import java.lang.reflect.Type;
import java.util.List;

public class WithDefinitions implements Generator {

    private final SchemaGenerator schemaGenerator;

    public WithDefinitions(GeneratorConfig config) {
        this.schemaGenerator = new SchemaGenerator(config.getConfig());
    }

    @Override
    public GeneratedResult generate(Type type, String schemaName) {
        var mainSchema = schemaGenerator.generateSchema(type);
        return new GeneratedResult(schemaName, mainSchema, List.of());
    }
}
