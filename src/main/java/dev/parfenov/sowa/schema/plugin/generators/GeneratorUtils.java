package dev.parfenov.sowa.schema.plugin.generators;

/**
 * Утилиты для работы с генератором JSON Schema.
 * <p>
 * Предоставляет вспомогательные методы для обработки ссылок и путей.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
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
