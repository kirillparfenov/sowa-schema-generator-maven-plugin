package dev.parfenov.sowa.schema.plugin.parsers.classes;

import dev.parfenov.sowa.schema.plugin.git.Git;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;

import java.util.List;

public class GitClassParser implements ClassParser {

    private final Git git;

    public GitClassParser(final ClassParserConfig config) {
        this.git = new Git(config.gitDiffCommand());
    }

    @Override
    public List<RestClass> parseAllRestClasses() {
        return List.of();
    }

    //todo закончить
}
