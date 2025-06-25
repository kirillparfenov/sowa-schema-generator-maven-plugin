package dev.parfenov.sowa.schema.plugin.sowa;

import dev.parfenov.sowa.schema.plugin.generator.GeneratedResult;
import org.springframework.http.HttpMethod;

public record SowaSchema(
        GeneratedResult request,
        GeneratedResult response,
        String restControllerName,
        HttpMethod httpMethod
) {}
