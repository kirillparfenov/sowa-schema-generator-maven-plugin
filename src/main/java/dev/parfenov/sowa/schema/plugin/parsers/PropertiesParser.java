/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.project.MavenProject;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

/**
 * Парсер конфигурационных файлов для извлечения настроек приложения.
 * Поддерживает парсинг файлов application.yml и application.properties
 * для получения контекстного пути сервлета.
 */
public final class PropertiesParser {

    private static final String APP_YAML = "application.y";
    private static final String APP_PROPERTIES = "application.properties";
    private static final String RESOURCES_PATH = "src/main/resources";
    private static final String SERVER = "server";
    private static final String SERVLET = "servlet";
    private static final String CONTEXT_PATH = "context-path";
    private static final String DEFAULT_CONTEXT_PATH = "";

    private PropertiesParser() {
    }

    /**
     * Извлекает контекстный путь сервлета из конфигурационных файлов проекта.
     * Ищет файлы application.yml или application.properties в директории src/main/resources
     * и извлекает значение server.servlet.context-path.
     *
     * @param project Maven проект для поиска конфигурационных файлов
     * @return контекстный путь сервлета или пустая строка, если не найден
     */
    public static String contextPath(MavenProject project) {
        return findContextPath(project).orElse(DEFAULT_CONTEXT_PATH);
    }

    /**
     * Ищет контекстный путь в конфигурационных файлах проекта.
     *
     * @param project Maven проект для поиска конфигурационных файлов
     * @return Optional с контекстным путем, если найден
     */
    public static Optional<String> findContextPath(MavenProject project) {
        if (project == null) {
            return Optional.empty();
        }

        var resourcesDir = getResourcesDirectory(project);
        if (!isValidDirectory(resourcesDir)) {
            return Optional.empty();
        }

        return findConfigFile(resourcesDir)
                .flatMap(PropertiesParser::parseConfigFile);
    }

    /**
     * Получает директорию resources проекта.
     *
     * @param project Maven проект
     * @return директория resources
     */
    private static File getResourcesDirectory(MavenProject project) {
        return new File(project.getBasedir(), RESOURCES_PATH);
    }

    /**
     * Проверяет, что директория существует и является директорией.
     *
     * @param directory директория для проверки
     * @return true, если директория валидна
     */
    private static boolean isValidDirectory(File directory) {
        return directory.exists() && directory.isDirectory();
    }

    /**
     * Находит первый подходящий конфигурационный файл в директории.
     *
     * @param resourcesDir директория resources
     * @return Optional с найденным файлом
     */
    private static Optional<File> findConfigFile(File resourcesDir) {
        var files = resourcesDir.listFiles();
        if (files == null) {
            return Optional.empty();
        }

        return Arrays.stream(files)
                .filter(File::isFile)
                .filter(PropertiesParser::isConfigFile)
                .findFirst();
    }

    /**
     * Проверяет, является ли файл конфигурационным.
     *
     * @param file файл для проверки
     * @return true, если файл является конфигурационным
     */
    private static boolean isConfigFile(File file) {
        var fileName = file.getName();
        return fileName.startsWith(APP_YAML) ||
                fileName.startsWith(APP_PROPERTIES);
    }

    /**
     * Парсит конфигурационный файл и извлекает контекстный путь.
     *
     * @param file конфигурационный файл
     * @return Optional с контекстным путем
     */
    private static Optional<String> parseConfigFile(File file) {
        try {
            var path = file.getName().startsWith(APP_YAML)
                    ? parseYaml(file)
                    : parseProperties(file);

            return Optional
                    .ofNullable(path)
                    .filter(StringUtils::hasText);
        } catch (ConfigurationParsingException e) {
            System.err.println("Ошибка при извлечении контекстного пути: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Парсит YAML файл и извлекает контекстный путь сервлета.
     * Ищет значение по пути: server.servlet.context-path
     *
     * @param file YAML файл для парсинга
     * @return контекстный путь сервлета или null, если не найден
     * @throws ConfigurationParsingException если произошла ошибка при чтении файла
     */
    private static String parseYaml(File file) {
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readTree(file)
                    .path(SERVER)
                    .path(SERVLET)
                    .path(CONTEXT_PATH)
                    .asText(null);
        } catch (IOException e) {
            throw new ConfigurationParsingException("Ошибка парсинга YAML файла: " + file.getName(), e);
        }
    }

    /**
     * Парсит Properties файл и извлекает контекстный путь сервлета.
     * Ищет значение свойства: server.servlet.context-path
     *
     * @param file Properties файл для парсинга
     * @return контекстный путь сервлета или null, если не найден
     * @throws ConfigurationParsingException если произошла ошибка при чтении файла
     */
    private static String parseProperties(File file) {
        try (var fis = new FileInputStream(file)) {
            var properties = new Properties();
            properties.load(fis);
            return properties.getProperty(String.join(".", SERVER, SERVLET, CONTEXT_PATH));
        } catch (IOException e) {
            throw new ConfigurationParsingException("Ошибка парсинга Properties файла: " + file.getName(), e);
        }
    }

    /**
     * Исключение, выбрасываемое при ошибках парсинга конфигурационных файлов.
     */
    public static class ConfigurationParsingException extends RuntimeException {
        public ConfigurationParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
