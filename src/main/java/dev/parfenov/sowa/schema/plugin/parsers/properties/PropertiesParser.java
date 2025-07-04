package dev.parfenov.sowa.schema.plugin.parsers.properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesParser {

    private static String CONTEXT_PATH = "";

    private PropertiesParser() {}

    public static String contextPath(MavenProject project) {
        if (!CONTEXT_PATH.isBlank()) return CONTEXT_PATH;

        var resources = new File(project.getBasedir(), "src/main/resources");
        if (!resources.exists() || !resources.isDirectory()) {
            return CONTEXT_PATH;
        }

        for (var resource : resources.listFiles()) {
            if (!resource.isFile()) {
                continue;
            }
            if (resource.getName().startsWith("application.y")) {
                CONTEXT_PATH = parseYaml(resource);
                return CONTEXT_PATH;
            } else if (resource.getName().startsWith("application.properties")) {
                CONTEXT_PATH =  parseProperties(resource);
                return CONTEXT_PATH;
            }

        }
        return CONTEXT_PATH;
    }

    private static String parseYaml(File file) {
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readTree(file)
                    .path("server")
                    .path("servlet")
                    .path("context-path")
                    .asText("");
        } catch (IOException e) {
            throw new RuntimeException("Ошибка парсинга " + file.getName(), e);
        }
    }

    private static String parseProperties(File file) {
        try {
            var properties = new Properties();
            properties.load(new FileInputStream(file));
            return properties.getProperty("server.servlet.context-path", "");
        } catch (IOException e) {
            throw new RuntimeException("Ошибка парсинга " + file.getName(), e);
        }
    }
}
