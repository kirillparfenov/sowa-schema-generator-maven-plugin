package dev.parfenov.sowa.schema.plugin.parsers.classes;

import java.util.List;

public record MethodPath(
        String fullPath,
        List<PathVariableParam> pathVariables
) {
}
