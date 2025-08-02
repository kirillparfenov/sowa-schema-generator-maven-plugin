/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporters.infra;

import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public record InfraConfig(
        DirectoriesBuilder directoriesBuilder,
        MavenProject project,
        String sowaProfileName
) {
}
