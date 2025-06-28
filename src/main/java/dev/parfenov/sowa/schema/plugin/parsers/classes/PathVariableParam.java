package dev.parfenov.sowa.schema.plugin.parsers.classes;

public record PathVariableParam(
        String paramName,
        Class<?> paramType
) {}
