package dev.parfenov.sowa.schema.plugin.generator;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public record GeneratedResult(
        String schemaName,
        ObjectNode jsonSchema,
        List<GeneratedResult> definitions
) {
}
