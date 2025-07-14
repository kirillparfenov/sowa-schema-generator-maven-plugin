package dev.parfenov.sowa.schema.plugin.generator;

import java.lang.reflect.Type;

public interface Generator {

    GeneratedResult generate(Type type, String schemaName);
}
