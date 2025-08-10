package dev.parfenov.sowa.schema.plugin.exporters.schemas;

import dev.parfenov.sowa.schema.plugin.config.ExportConfig;
import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import dev.parfenov.sowa.schema.plugin.generators.dto.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Экспортер JSON схем в файловую систему.
 * Отвечает за сохранение сгенерированных схем запросов и ответов в соответствующие директории.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
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
     * @param restControllers контроллеры для экспорта
     * @throws SchemaExportException если произошла ошибка при экспорте
     */
    public void export(List<ClassModel> restControllers) {
        if (CollectionUtils.isEmpty(restControllers)) {
            return;
        }
        restControllers
                .stream()
                .map(ClassModel::getMethods)
                .forEach(this::exportSchemas);
    }

    /**
     * Экспортирует схемы запросов и ответов в соответствующие директории.
     *
     * @param methodModels REST методы для экспорта
     */
    private void exportSchemas(List<MethodModel> methodModels) {
        methodModels.stream()
                .filter(Objects::nonNull)
                .forEach(method -> {
                    exportRequestSchema(method);
                    exportResponseSchema(method);
                });
    }

    /**
     * Экспортирует схему запроса в соответствующую директорию.
     *
     * @param method REST метод для экспорта
     */
    private void exportRequestSchema(MethodModel method) {
        if (method.getRequest().canExport()) {
            exportSchemaWithDefinitions(
                    directoriesBuilder.requestDir(),
                    method.getRequest().getSchema()
            );
        }

    }

    /**
     * Экспортирует схему ответа в соответствующую директорию.
     *
     * @param method REST метод для экспорта
     */
    private void exportResponseSchema(MethodModel method) {
        if (method.getResponse().canExport()) {
            exportSchemaWithDefinitions(
                    directoriesBuilder.responseDir(),
                    method.getResponse().getSchema()
            );
        }
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
