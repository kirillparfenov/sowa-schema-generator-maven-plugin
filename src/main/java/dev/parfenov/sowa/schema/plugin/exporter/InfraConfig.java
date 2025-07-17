package dev.parfenov.sowa.schema.plugin.exporter;

import org.apache.maven.project.MavenProject;

public record InfraConfig(
        MavenProject project,
        String sowaProfileName
) {
}
