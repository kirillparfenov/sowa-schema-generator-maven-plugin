package dev.parfenov.sowa.schema.plugin.exporter.infra;

import org.apache.maven.project.MavenProject;
import org.springframework.lang.Nullable;

import java.util.Set;

public record InfraConfig(
        MavenProject project,
        String sowaProfileName,
        @Nullable
        Set<String> gitDiff
) {
}
