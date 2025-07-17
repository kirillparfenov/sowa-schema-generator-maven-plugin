package dev.parfenov.sowa.schema.plugin.parsers;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Paths;

public class PackageParser {
    private PackageParser() {
    }

    /**
     * @return source package в формате src/main/java (разделитель платформозависимый)
     */
    public static String srcRoot(MavenProject project) {
        var baseDir = Paths.get(project.getBasedir().getAbsolutePath());
        var sourceDir = Paths.get(project.getBuild().getSourceDirectory());
        return baseDir.relativize(sourceDir).toString();
    }

    public static String pathToClass(String srcRoot, String classPath) {
        return Paths.get(srcRoot)
                .relativize(Paths.get(classPath))
                .toString()
                .replace(File.separatorChar, '.');
    }
}
