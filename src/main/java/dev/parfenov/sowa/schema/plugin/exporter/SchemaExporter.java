package dev.parfenov.sowa.schema.plugin.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.parfenov.sowa.schema.plugin.generator.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.sowa.SowaSchema;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.*;

public class SchemaExporter {

    private final ExportConfig config;
    private final ObjectMapper mapper;

    public SchemaExporter(ExportConfig config) {
        this.config = config;
        this.mapper = new ObjectMapper();
    }

    /**
     * Экспорт схем
     *
     * @param sowaSchemas схемы, готовые для экспорта
     */
    public void export(List<SowaSchema> sowaSchemas) {
        if (CollectionUtils.isEmpty(sowaSchemas)) {
            return;
        }
        var sowaDir = new File(config.project().getBuild().getDirectory(), SOWA_DIRECTORY);
        var requestDir = buildDirectory(sowaDir, REQUEST_DIRECTORY);
        var responseDir = buildDirectory(sowaDir, RESPONSE_DIRECTORY);
        try {
            exportJson(requestDir, responseDir, sowaSchemas);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка во время экспорта в .json: " + sowaSchemas, e);
        }
    }

    private File buildDirectory(File sowaDir, String dirName) {
        var newDirectory = new File(sowaDir, dirName);
        newDirectory.mkdirs();
        return newDirectory;
    }

    private void exportJson(File requestDir, File responseDir, List<SowaSchema> sowaSchemas) throws IOException {
        for (var sowaSchema : sowaSchemas) {
            exportJson(requestDir, sowaSchema.getRequest());
            exportJson(responseDir, sowaSchema.getResponse());
        }
    }

    private void exportJson(File directory, GeneratedResult result) throws IOException {
        if (result == null) {
            return;
        }

        var mainSchema = buildFile(directory, result);
        writeFile(mainSchema, result.jsonSchema());

        if (CollectionUtils.isEmpty(result.definitions())) {
            return;
        }

        for (var definition : result.definitions()) {
            var definitionSchema = buildFile(directory, definition);
            writeFile(definitionSchema, definition.jsonSchema());
        }
    }

    private File buildFile(File directory, GeneratedResult generatedResult) {
        return new File(directory, generatedResult.schemaName().concat(".json"));
    }

    private void writeFile(File file, ObjectNode node) throws IOException {
        if (!file.exists()) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, node);
            config.log().info("Сгенерирована схема: %s".formatted(file.getPath()));
        }
    }
}
