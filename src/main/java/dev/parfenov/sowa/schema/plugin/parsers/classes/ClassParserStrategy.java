package dev.parfenov.sowa.schema.plugin.parsers.classes;

public class ClassParserStrategy {
    private ClassParserStrategy() {}

    public static ClassParser getClassParser(ClassParserConfig config) {
        if (config.onlyGitDiff()) {
            return new GitClassParser(config);
        }

        return new SimpleParser(config);
    }
}
