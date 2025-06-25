package dev.parfenov.sowa.schema.plugin.classparser;

public class ClassParserStrategy {
    private ClassParserStrategy() {}

    public static ClassParser getClassParser(ClassParserConfig config) {
        if (config.onlyGitDiff()) {
            return new GitClassParser(config);
        }

        return new AllClassPathParser(config);
    }
}
