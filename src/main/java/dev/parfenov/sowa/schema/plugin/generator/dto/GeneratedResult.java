/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.generator.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Результат генерации JSON Schema.
 *
 * @param schemaName  имя схемы
 * @param jsonSchema  основная JSON Schema в виде ObjectNode
 * @param definitions список дополнительных определений (если используется режим раздельных определений)
 */
public record GeneratedResult(
        String schemaName,
        ObjectNode jsonSchema,
        List<GeneratedResult> definitions
) {
}
