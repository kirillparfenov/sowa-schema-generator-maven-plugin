package dev.parfenov.sowa.schema.plugin.exporter;

import dev.parfenov.sowa.schema.plugin.sowa.SowaSchema;

import java.io.IOException;
import java.util.List;

public interface Export {

    /**
     * Экспорт схем
     *
     * @param sowaSchemaList схемы, готовые для экспорта
     */
    void export(List<SowaSchema> sowaSchemaList);
}
