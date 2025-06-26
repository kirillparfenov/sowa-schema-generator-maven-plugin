package dev.parfenov.sowa.schema.plugin.exporter.infrastructure;

import dev.parfenov.sowa.schema.plugin.parsers.classes.ClassMethod;

import java.util.List;

public interface InfrastructureExporter {

    /**
     * Экспорт обвязки SOWA
     */
    void export(List<ClassMethod> classMethods);
}
