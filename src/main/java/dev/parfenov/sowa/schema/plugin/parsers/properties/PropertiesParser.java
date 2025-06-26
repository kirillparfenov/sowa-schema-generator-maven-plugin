package dev.parfenov.sowa.schema.plugin.parsers.properties;

import java.io.File;

public class PropertiesParser {

    public String getServletContextPath(File... propertiesFiles) {
        for (File propertiesFile : propertiesFiles) {
            if (!propertiesFile.exists()) continue;

            var path = propertiesFile.getPath();
            return path.endsWith(".properties")
                    ? parseProperties(propertiesFile)
                    : parseYaml(propertiesFile);
        }
        return "";
    }

    private String parseYaml(File file) {
        //todo finish
        return "";
    }

    private String parseProperties(File file) {
        //todo finish
        return "";
    }
}
