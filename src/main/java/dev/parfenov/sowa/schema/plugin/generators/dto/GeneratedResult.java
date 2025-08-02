package dev.parfenov.sowa.schema.plugin.generators.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Результат генерации JSON Schema.
 *
 * @param schemaName  имя схемы
 * @param jsonSchema  основная JSON Schema в виде ObjectNode
 * @param definitions список дополнительных определений (если используется режим раздельных определений)
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public record GeneratedResult(
        String schemaName,
        ObjectNode jsonSchema,
        List<GeneratedResult> definitions
) {
}
