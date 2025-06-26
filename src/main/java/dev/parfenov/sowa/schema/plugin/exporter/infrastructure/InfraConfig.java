package dev.parfenov.sowa.schema.plugin.exporter.infrastructure;

import org.apache.maven.project.MavenProject;

public record InfraConfig(
        MavenProject project,
        String sowaProfileName
) {
}
