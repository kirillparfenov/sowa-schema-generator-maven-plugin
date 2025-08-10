package dev.parfenov.sowa.schema.plugin.exporters.schemas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * Утилитный класс для записи JSON файлов.
 * Обеспечивает безопасную запись JSON объектов в файловую систему с логированием.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class JsonFileWriter {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Log logger;

    public JsonFileWriter(Log logger) {
        this.logger = logger;
    }

    /**
     * Записывает JSON узел в файл, если файл не существует.
     * Выполняет форматированную запись с отступами для читаемости.
     *
     * @param file     файл для записи
     * @param jsonNode JSON узел для записи
     * @throws JsonWriteException если произошла ошибка при записи
     */
    public void writeJsonFile(File file, JsonNode jsonNode) {
        if (file.exists()) {
            return; // Файл уже существует, пропускаем запись
        }

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, jsonNode);
            if (logger != null) {
                logger.info("Записан JSON файл: %s".formatted(file.getPath()));
            } else {
                System.out.println("Записан JSON файл: %s".formatted(file.getPath()));
            }
        } catch (IOException e) {
            throw new JsonWriteException(
                    "Ошибка записи JSON в файл: " + file.getPath(), e
            );
        }
    }

    /**
     * Исключение, выбрасываемое при ошибках записи JSON файлов.
     */
    public static class JsonWriteException extends RuntimeException {
        public JsonWriteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 