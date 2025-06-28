package dev.parfenov.sowa.schema.plugin.exporter.infra;

import org.apache.maven.project.MavenProject;

public record InfraConfig(
        MavenProject project,
        String sowaProfileName
) {
}
