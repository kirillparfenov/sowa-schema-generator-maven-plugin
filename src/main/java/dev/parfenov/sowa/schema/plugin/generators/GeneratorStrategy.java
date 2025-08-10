package dev.parfenov.sowa.schema.plugin.generators;

import dev.parfenov.sowa.schema.plugin.config.GeneratorConfig;

/**
 * Стратегия для выбора типа генератора JSON Schema.
 * <p>
 * Предоставляет фабричный метод для создания соответствующего генератора
 * в зависимости от конфигурации.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class GeneratorStrategy {
    private GeneratorStrategy() {
    }

    /**
     * Возвращает генератор в зависимости от конфигурации.
     *
     * @param config конфигурация генератора
     * @return генератор с раздельными определениями если extractDefinitions=true,
     * иначе генератор со встроенными определениями
     */
    public static Generator getGenerator(GeneratorConfig config) {
        if (config.isExtractDefinitions()) {
            return new SeparateDefinitions(config);
        }

        return new WithDefinitions(config);
    }
}
