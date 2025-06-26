package dev.parfenov.sowa.schema.plugin.exporter.schemas;

import java.util.Optional;

import static dev.parfenov.sowa.schema.plugin.exporter.schemas.ExportTo.TARGET;

public class ExportStrategy {

    private ExportStrategy() {}

    /**
     * В зависимости от настроек вернет реализацию {@link SchemaExporter}
     *
     * @param config настройки экспорта
     * @return реализация {@link SchemaExporter}
     */
    public static Optional<SchemaExporter> getExporter(ExportConfig config) {
        if (config.exportTo().equals(TARGET)) {
            return Optional.of(new ExportToTarget(config));
        }
        return Optional.empty();
    }
}
