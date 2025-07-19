/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporter;

import org.apache.maven.project.MavenProject;

public record InfraConfig(
        DirectoriesBuilder directoriesBuilder,
        MavenProject project,
        String sowaProfileName
) {
}
