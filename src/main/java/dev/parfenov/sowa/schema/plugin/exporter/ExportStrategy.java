package dev.parfenov.sowa.schema.plugin.exporter;

import java.util.Optional;

import static dev.parfenov.sowa.schema.plugin.exporter.ExportTo.TARGET;

public class ExportStrategy {

    private ExportStrategy() {}

    /**
     * В зависимости от настроек вернет реализацию {@link Export}
     *
     * @param config настройки экспорта
     * @return реализация {@link Export}
     */
    public static Optional<Export> getExporter(ExportConfig config) {
        if (config.exportTo().equals(TARGET)) {
            return Optional.of(new ExportToTarget(config));
        }
        return Optional.empty();
    }
}
