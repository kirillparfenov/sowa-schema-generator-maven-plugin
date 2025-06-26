package dev.parfenov.sowa.schema.plugin.exporter.schemas;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public record ExportConfig(
        ExportTo exportTo,
        MavenProject project,
        Log log
) {

    public static ExportConfig toTarget(MavenProject project, Log log) {
        return new ExportConfig(ExportTo.TARGET, project, log);
    }
}
