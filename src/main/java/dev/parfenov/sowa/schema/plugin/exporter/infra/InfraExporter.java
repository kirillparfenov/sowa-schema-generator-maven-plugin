package dev.parfenov.sowa.schema.plugin.exporter.infra;

import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;

import java.util.List;

public interface InfraExporter {

    /**
     * Экспорт обвязки SOWA
     */
    void export(List<RestClass> restClassMethods);
}
