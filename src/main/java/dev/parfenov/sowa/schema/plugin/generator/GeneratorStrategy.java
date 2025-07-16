package dev.parfenov.sowa.schema.plugin.generator;

public class GeneratorStrategy {
    private GeneratorStrategy() {}

    public static Generator getGenerator(GeneratorConfig config) {
        if (config.isExtractDefinitions()) {
            return new SeparateDefinitions(config);
        }

        return new WithDefinitions(config);
    }
}
