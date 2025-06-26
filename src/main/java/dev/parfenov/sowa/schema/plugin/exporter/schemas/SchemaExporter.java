package dev.parfenov.sowa.schema.plugin.exporter.schemas;

import dev.parfenov.sowa.schema.plugin.sowa.SowaSchema;

import java.util.List;

public interface SchemaExporter {

    /**
     * Экспорт схем
     *
     * @param sowaSchemaList схемы, готовые для экспорта
     */
    void export(List<SowaSchema> sowaSchemaList);
}
