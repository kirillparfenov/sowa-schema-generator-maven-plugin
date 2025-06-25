package dev.parfenov.sowa.schema.plugin.sowa;

import dev.parfenov.sowa.schema.plugin.classparser.ClassMethod;

import java.util.List;

public interface SowaSchemaGenerator {
    List<SowaSchema> generateSchema(List<ClassMethod> restMethod);
}
