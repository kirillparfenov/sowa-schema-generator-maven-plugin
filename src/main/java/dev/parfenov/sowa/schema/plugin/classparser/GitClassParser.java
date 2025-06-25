package dev.parfenov.sowa.schema.plugin.classparser;

import dev.parfenov.sowa.schema.plugin.git.Git;
import org.apache.maven.project.MavenProject;

public class GitClassParser extends AbstractClassParser {

    private final Git git;

    public GitClassParser(final ClassParserConfig config) {
        super(config);
        this.git = new Git(config.gitDiffCommand());
    }

    //todo закончить
}
