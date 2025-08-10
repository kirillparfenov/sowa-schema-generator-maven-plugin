package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.config.ConfigurationFactory;

/**
 * Работа с плагином через cli.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-10
 */
public class CLI {

    /**
     * Точка входа для автономного запуска плагина.
     * Используется для тестирования и отладки вне Maven контекста.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        ConfigurationFactory.createCliPluginFromSystemProperties().start();
    }
}
