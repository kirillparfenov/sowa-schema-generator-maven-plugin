package dev.parfenov.sowa.schema.plugin.generator;

import java.util.Map;
import java.util.UUID;

/**
 * Паттерны регулярных выражений для различных типов данных.
 * <p>
 * Предоставляет соответствие между Java классами и их regex представлениями
 * для валидации в путях URL.
 */
public class Regex {
    private static final String DEFAULT_REGEX = ".{0,255}";
    private static final Map<String, String> CLASS_REGEX = Map.of(
            UUID.class.getName(), "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    private Regex() {
    }

    /**
     * Возвращает regex паттерн для указанного класса.
     *
     * @param className полное имя Java класса
     * @return regex паттерн для класса или паттерн по умолчанию
     */
    public static String getRegexOrDefault(String className) {
        return CLASS_REGEX.getOrDefault(className, DEFAULT_REGEX);
    }
}
