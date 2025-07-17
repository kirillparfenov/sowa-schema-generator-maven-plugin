package dev.parfenov.sowa.schema.plugin.parsers.classes;

import org.apache.maven.project.MavenProject;

public record ClassParserConfig(
        MavenProject project,
        String projectBasePackage,
        boolean onlyGitDiff,
        String branchDiffWith
) {}
