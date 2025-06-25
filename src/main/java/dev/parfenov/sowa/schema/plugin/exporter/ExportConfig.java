package dev.parfenov.sowa.schema.plugin.exporter;

import org.apache.maven.project.MavenProject;

public record ExportConfig(
        ExportTo exportTo,
        MavenProject project
) {

    public static ExportConfig toTarget(MavenProject project) {
        return new ExportConfig(ExportTo.TARGET, project);
    }
}
