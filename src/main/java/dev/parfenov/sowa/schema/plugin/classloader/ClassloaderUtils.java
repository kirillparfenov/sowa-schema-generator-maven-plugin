package dev.parfenov.sowa.schema.plugin.classloader;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ClassloaderUtils {
    private ClassloaderUtils() {}

    public static String baseProjectPackage(MavenProject project) {
        var classesDir = Path.of(project.getBuild().getOutputDirectory());
        try (var stream = Files.walk(classesDir)) {
            var allProjectClassNames = stream
                    .filter(path -> path.toString().endsWith(".class"))
                    .map(classesDir::relativize)
                    .map(path -> path.toString().replace(File.separator, "."))
                    .map(path -> path.substring(0, path.lastIndexOf('.')))
                    .toList();

            var packageTokens = allProjectClassNames.get(0).split("\\.");
            var basePackage = new StringBuilder();

            for (var packageToken : packageTokens) {
                var currentToken = basePackage.toString().concat(packageToken).concat(".");
                if (allHasToken(allProjectClassNames, currentToken)) {
                    basePackage.append(packageToken).append(".");
                } else {
                    basePackage.deleteCharAt(basePackage.length() - 1);
                    break;
                }
            }

            return basePackage.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean allHasToken(List<String> fullClassNames, String token) {
        return fullClassNames
                .stream()
                .allMatch(className -> className.startsWith(token));
    }
}
