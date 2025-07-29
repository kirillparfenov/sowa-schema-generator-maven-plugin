package dev.parfenov.sowa.schema.plugin.generators.config;

/**
 * Константы для валидации схем.
 * <p>
 * Содержит предустановленные значения для ограничений валидации.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025
 */
public final class ValidationConstants {

    /**
     * Максимальный размер массива
     */
    public static final Integer ARRAY_MAX_SIZE = 1000;

    /**
     * Максимальная длина строки по умолчанию
     */
    public static final Integer DEFAULT_STRING_MAX_LENGTH = 300;

    /**
     * Максимальная длина UUID
     */
    public static final Integer UUID_MAX_LENGTH = 36;

    /**
     * Порог для округления длины строк
     */
    public static final Integer SMALL_LENGTH_STEP = 10;

    /**
     * Порог для округления больших длин строк
     */
    public static final Integer LARGE_LENGTH_STEP = 100;

    /**
     * Граница между малыми и большими длинами
     */
    public static final Integer LENGTH_BOUNDARY = 100;

    private ValidationConstants() {
    }
} 