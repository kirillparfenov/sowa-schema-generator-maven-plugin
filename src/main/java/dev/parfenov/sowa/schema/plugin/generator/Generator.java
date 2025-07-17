package dev.parfenov.sowa.schema.plugin.generator;

import java.lang.reflect.Type;

/**
 * Интерфейс для генераторов JSON Schema.
 * <p>
 * Определяет контракт для создания JSON Schema из Java типов.
 */
public interface Generator {

    /**
     * Генерирует JSON Schema для указанного типа.
     *
     * @param type       Java тип для генерации схемы
     * @param schemaName имя создаваемой схемы
     * @return результат генерации содержащий схему и определения
     */
    GeneratedResult generate(Type type, String schemaName);
}
