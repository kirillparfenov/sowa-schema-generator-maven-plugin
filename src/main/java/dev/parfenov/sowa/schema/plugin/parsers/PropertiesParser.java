package dev.parfenov.sowa.schema.plugin.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Парсер конфигурационных файлов для извлечения настроек приложения.
 * Поддерживает парсинг файлов application.yml и application.properties
 * для получения контекстного пути сервлета.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public final class PropertiesParser {

    private static final String BOOT_INF = "BOOT-INF/classes/";
    private static final String APP_YAML = "application.y";
    private static final String APP_PROPERTIES = "application.properties";
    private static final String RESOURCES_PATH = "src/main/resources";
    private static final String SERVER = "server";
    private static final String SERVLET = "servlet";
    private static final String CONTEXT_PATH = "context-path";
    private static final String DEFAULT_CONTEXT_PATH = "";
    private static final String PROPERTY_PATH = String.join(".", SERVER, SERVLET, CONTEXT_PATH);

    private PropertiesParser() {
    }

    /**
     * Извлекает контекстный путь сервлета из конфигурационных файлов проекта.
     * Ищет файлы application.yml или application.properties в директории src/main/resources
     * и извлекает значение server.servlet.context-path.
     *
     * @param baseDir базовая директория проекта
     * @return контекстный путь сервлета или пустая строка, если не найден
     */
    public static String contextPath(File baseDir) {
        return findContextPath(baseDir).orElse(DEFAULT_CONTEXT_PATH);
    }

    /**
     * Извлекает контекстный путь сервлета из uber JAR файла.
     *
     * @param uberJarPath путь к uber JAR файлу
     * @return контекстный путь сервлета или пустая строка, если не найден
     */
    public static String contextPath(String uberJarPath) {
        return findContextPath(uberJarPath).orElse(DEFAULT_CONTEXT_PATH);
    }

    /**
     * Ищет контекстный путь в конфигурационных файлах проекта.
     *
     * @param baseDir базовая директория проекта
     * @return Optional с контекстным путем, если найден
     */
    public static Optional<String> findContextPath(File baseDir) {
        if (baseDir == null) {
            return Optional.empty();
        }

        var resourcesDir = getResourcesDirectory(baseDir);
        if (!isValidDirectory(resourcesDir)) {
            return Optional.empty();
        }

        return findConfigFile(resourcesDir)
                .flatMap(PropertiesParser::parseConfigFile);
    }

    /**
     * Ищет контекстный путь в uber JAR файле.
     *
     * @param uberJarPath путь к uber JAR файлу
     * @return Optional с контекстным путем, если найден
     */
    public static Optional<String> findContextPath(String uberJarPath) {
        try (JarFile jarFile = new JarFile(uberJarPath)) {
            return parseJarFile(jarFile);
        } catch (IOException e) {
            throw new ConfigurationParsingException("Ошибка чтения конфиг-файла из uber-jar: " + uberJarPath, e);
        }
    }

    /**
     * Парсит JAR файл в поисках конфигурационных файлов.
     *
     * @param jarFile JAR файл для парсинга
     * @return Optional с найденным контекстным путем
     */
    private static Optional<String> parseJarFile(JarFile jarFile) {
        var entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            var result = parseJarEntry(jarFile, entry);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    /**
     * Парсит отдельную запись JAR файла.
     *
     * @param jarFile JAR файл
     * @param entry   запись в JAR файле
     * @return Optional с найденным контекстным путем
     */
    private static Optional<String> parseJarEntry(JarFile jarFile, JarEntry entry) {
        var entryName = entry.getName();

        try {
            if (entryName.startsWith(BOOT_INF + APP_PROPERTIES)) {
                try (var inputStream = jarFile.getInputStream(entry)) {
                    return Optional.ofNullable(parseProperties(inputStream));
                }
            } else if (entryName.startsWith(BOOT_INF + APP_YAML)) {
                try (var inputStream = jarFile.getInputStream(entry)) {
                    return Optional.ofNullable(parseYaml(inputStream));
                }
            }
        } catch (IOException e) {
            throw new ConfigurationParsingException("Ошибка чтения записи JAR: " + entryName, e);
        }

        return Optional.empty();
    }

    /**
     * Получает директорию resources проекта.
     *
     * @param baseDir базовая директория
     * @return директория resources
     */
    private static File getResourcesDirectory(File baseDir) {
        return new File(baseDir, RESOURCES_PATH);
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
            var contextPath = file.getName().startsWith(APP_YAML)
                    ? parseYamlFile(file)
                    : parsePropertiesFile(file);

            return Optional
                    .ofNullable(contextPath)
                    .filter(StringUtils::hasText);
        } catch (ConfigurationParsingException e) {
            System.err.println("Ошибка при извлечении контекстного пути: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Парсит YAML файл и извлекает контекстный путь сервлета.
     * Ищет значение по пути: server.servlet.context-path
     *
     * @param file YAML файл для парсинга
     * @return контекстный путь сервлета или null, если не найден
     * @throws ConfigurationParsingException если произошла ошибка при чтении файла
     */
    private static String parseYamlFile(File file) {
        try (var fileInputStream = new FileInputStream(file)) {
            return parseYaml(fileInputStream);
        } catch (IOException e) {
            throw new ConfigurationParsingException("Ошибка чтения YAML файла: " + file.getName(), e);
        }
    }

    /**
     * Парсит YAML из InputStream и извлекает контекстный путь сервлета.
     *
     * @param inputStream поток для чтения YAML
     * @return контекстный путь сервлета или null, если не найден
     * @throws ConfigurationParsingException если произошла ошибка при парсинге
     */
    private static String parseYaml(InputStream inputStream) {
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readTree(inputStream)
                    .path(SERVER)
                    .path(SERVLET)
                    .path(CONTEXT_PATH)
                    .asText(null);
        } catch (IOException e) {
            throw new ConfigurationParsingException("Ошибка парсинга YAML из inputStream", e);
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
    private static String parsePropertiesFile(File file) {
        try (var fileInputStream = new FileInputStream(file)) {
            return parseProperties(fileInputStream);
        } catch (IOException e) {
            throw new ConfigurationParsingException("Ошибка чтения Properties файла: " + file.getName(), e);
        }
    }

    /**
     * Парсит Properties из InputStream и извлекает контекстный путь сервлета.
     *
     * @param inputStream поток для чтения Properties
     * @return контекстный путь сервлета или null, если не найден
     * @throws ConfigurationParsingException если произошла ошибка при парсинге
     */
    private static String parseProperties(InputStream inputStream) {
        try {
            var properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(PROPERTY_PATH);
        } catch (IOException e) {
            throw new ConfigurationParsingException("Ошибка парсинга Properties из inputStream", e);
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