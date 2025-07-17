package dev.parfenov.sowa.schema.plugin.exporter.schemas;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.springframework.lang.Nullable;

import java.util.Set;

public record ExportConfig(
        MavenProject project,
        Log log
) {}
