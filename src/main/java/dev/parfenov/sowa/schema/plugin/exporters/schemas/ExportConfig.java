package dev.parfenov.sowa.schema.plugin.exporters.schemas;

import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public record ExportConfig(
        DirectoriesBuilder directoriesBuilder,
        MavenProject project,
        Log log
) {
}
