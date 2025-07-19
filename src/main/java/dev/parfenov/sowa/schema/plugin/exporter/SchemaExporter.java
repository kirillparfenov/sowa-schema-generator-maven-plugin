/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporter;

import dev.parfenov.sowa.schema.plugin.generator.dto.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.sowa.SowaSchema;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Экспортер JSON схем в файловую систему.
 * Отвечает за сохранение сгенерированных схем запросов и ответов в соответствующие директории.
 */
public class SchemaExporter {

    private static final String JSON_EXTENSION = ".json";

    private final DirectoriesBuilder directoriesBuilder;
    private final JsonFileWriter jsonFileWriter;

    public SchemaExporter(final ExportConfig config) {
        this.directoriesBuilder = config.directoriesBuilder();
        this.jsonFileWriter = new JsonFileWriter(config.log());
    }

    /**
     * Экспортирует коллекцию схем в файловую систему.
     *
     * @param sowaSchemas схемы для экспорта
     * @throws SchemaExportException если произошла ошибка при экспорте
     */
    public void export(List<SowaSchema> sowaSchemas) {
        if (CollectionUtils.isEmpty(sowaSchemas)) {
            return;
        }

        exportSchemas(sowaSchemas);
    }

    /**
     * Экспортирует схемы запросов и ответов в соответствующие директории.
     *
     * @param sowaSchemas коллекция схем для экспорта
     */
    private void exportSchemas(List<SowaSchema> sowaSchemas) {
        var requestDir = directoriesBuilder.requestDir();
        var responseDir = directoriesBuilder.responseDir();

        sowaSchemas.stream()
                .filter(Objects::nonNull)
                .forEach(schema -> {
                    exportSchemaWithDefinitions(requestDir, schema.getRequest());
                    exportSchemaWithDefinitions(responseDir, schema.getResponse());
                });
    }

    /**
     * Экспортирует схему вместе с её определениями.
     *
     * @param directory директория для экспорта
     * @param result    результат генерации схемы
     */
    private void exportSchemaWithDefinitions(File directory, GeneratedResult result) {
        Optional.ofNullable(result)
                .ifPresent(schema -> {
                    exportSingleSchema(directory, schema);
                    exportDefinitions(directory, schema);
                });
    }

    /**
     * Экспортирует одну схему.
     *
     * @param directory       директория для экспорта
     * @param generatedResult результат генерации схемы
     */
    private void exportSingleSchema(File directory, GeneratedResult generatedResult) {
        var schemaFile = createSchemaFile(directory, generatedResult);
        jsonFileWriter.writeJsonFile(schemaFile, generatedResult.jsonSchema());
    }

    /**
     * Экспортирует определения схемы.
     *
     * @param directory       директория для экспорта
     * @param generatedResult результат генерации с определениями
     */
    private void exportDefinitions(File directory, GeneratedResult generatedResult) {
        Optional.ofNullable(generatedResult.definitions())
                .filter(Predicate.not(CollectionUtils::isEmpty))
                .ifPresent(definitions ->
                        definitions.forEach(definition -> exportSingleSchema(directory, definition))
                );
    }

    /**
     * Создает файл для схемы в указанной директории.
     *
     * @param directory       директория
     * @param generatedResult результат генерации
     * @return файл для схемы
     */
    private File createSchemaFile(File directory, GeneratedResult generatedResult) {
        var fileName = createFileName(generatedResult.schemaName());
        return directoriesBuilder.buildFile(directory, fileName);
    }

    /**
     * Создает имя файла с расширением JSON.
     *
     * @param schemaName имя схемы
     * @return имя файла с расширением
     */
    private String createFileName(String schemaName) {
        return schemaName + JSON_EXTENSION;
    }


    /**
     * Исключение, выбрасываемое при ошибках экспорта схем.
     */
    public static class SchemaExportException extends RuntimeException {
        public SchemaExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
