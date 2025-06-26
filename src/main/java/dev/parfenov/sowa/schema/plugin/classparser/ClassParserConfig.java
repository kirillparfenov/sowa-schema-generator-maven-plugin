package dev.parfenov.sowa.schema.plugin.classparser;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public record ClassParserConfig(
        boolean onlyGitDiff,
        String gitDiffCommand,
        MavenProject project,
        Log logger
) {
}
