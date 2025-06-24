package dev.parfenov.sowa.schema.plugin.generator;

import dev.parfenov.sowa.schema.plugin.classparser.ClassParser;
import org.apache.maven.project.MavenProject;

public class SowaSchemaGeneratorImpl implements SowaSchemaGenerator {

    private final ClassParser classParser;
    protected final SchemaGeneratorService generatorService;

    public SowaSchemaGeneratorImpl(
            final ClassParser classParser
    ) {
        this.classParser = classParser;
        this.generatorService = new SchemaGeneratorService();
    }

    @Override
    public void generateSchema() {

    }
}
