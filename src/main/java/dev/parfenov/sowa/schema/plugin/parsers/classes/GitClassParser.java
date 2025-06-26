package dev.parfenov.sowa.schema.plugin.parsers.classes;

import dev.parfenov.sowa.schema.plugin.git.Git;

public class GitClassParser extends AbstractClassParser {

    private final Git git;

    public GitClassParser(final ClassParserConfig config) {
        super(config);
        this.git = new Git(config.gitDiffCommand());
    }

    //todo закончить
}
