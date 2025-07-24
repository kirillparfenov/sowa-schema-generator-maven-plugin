/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.generators;

/**
 * Утилиты для работы с генератором JSON Schema.
 * <p>
 * Предоставляет вспомогательные методы для обработки ссылок и путей.
 */
public class GeneratorUtils {

    private static final String PREFIX = "./";
    private static final String SUFFIX = ".json";

    private GeneratorUtils() {
    }

    /**
     * Изменяет путь ссылки на локальный формат.
     * <p>
     * Преобразует ссылку вида "#/definitions/SomeName" в "./SomeName.json".
     *
     * @param refValue исходная ссылка
     * @return новый путь в локальном формате
     */
    public static String changeRefPath(String refValue) {
        return PREFIX
                .concat(refValue.substring(refValue.lastIndexOf("/") + 1))
                .concat(SUFFIX);
    }
}
