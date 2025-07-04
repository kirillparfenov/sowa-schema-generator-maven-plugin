package dev.parfenov.sowa.schema.plugin.sowa;

import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClassMethod;

import java.util.List;

public interface SowaSchemaGenerator {
    List<SowaSchema> generateSchema(List<RestClass> restClasses);
}
