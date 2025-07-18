/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporter;

import java.io.File;

/**
 * Константы директорий для экспорта файлов.
 * <p>
 * Определяет стандартные пути для различных типов экспортируемых файлов.
 */
public class ExportDirectories {
    /**
     * Основная директория Sowa
     */
    public static final String SOWA_DIRECTORY = File.separator + "sowa";

    /**
     * Директория для схем запросов
     */
    public static final String REQUEST_DIRECTORY = File.separator + "request";

    /**
     * Директория для схем ответов
     */
    public static final String RESPONSE_DIRECTORY = File.separator + "response";

    /**
     * Директория для файлов инфраструктуры
     */
    public static final String INFRASTRUCTURE_DIRECTORY = "";

    private ExportDirectories() {
    }
}
