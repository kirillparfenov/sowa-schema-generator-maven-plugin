package dev.parfenov.sowa.schema.plugin.classparser;

import dev.parfenov.sowa.schema.plugin.git.Git;
import org.apache.maven.project.MavenProject;

public class GitClassParser extends AbstractClassParser {

    private final Git git;

    public GitClassParser(String gitDiffCommand, MavenProject project) {
        super(project);
        this.git = new Git(gitDiffCommand);
    }
}
