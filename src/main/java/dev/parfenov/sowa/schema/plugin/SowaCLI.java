package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.config.ConfigurationFactory;

/**
 * Командная строка (CLI) для автономного запуска генератора Sowa схем.
 * Используется для запуска приложения вне контекста Maven или Gradle.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-10
 */
public class SowaCLI {

    /**
     * Точка входа для автономного запуска приложения.
     * Загружает конфигурацию из системных свойств и запускает генерацию схем.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        ConfigurationFactory.createCliPluginFromSystemProperties().start();
    }
}