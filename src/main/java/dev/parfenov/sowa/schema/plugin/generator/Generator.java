package dev.parfenov.sowa.schema.plugin.generator;

import com.github.victools.jsonschema.generator.SchemaGenerator;

public class Generator {

    private final SchemaGenerator schemaGenerator;

    public Generator(GeneratorConfig config) {
        this.schemaGenerator = new SchemaGenerator(config.getConfig());
    }

    public SchemaGenerator getSchemaGenerator() {
        return schemaGenerator;
    }
}
