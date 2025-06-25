package dev.parfenov.sowa.schema.plugin.sowa;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.parfenov.sowa.schema.plugin.classparser.ClassMethod;
import dev.parfenov.sowa.schema.plugin.generator.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.generator.Generator;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class SowaSchemaGeneratorImpl implements SowaSchemaGenerator {

    private static final String REQUEST_SUFFIX = "_request";
    private static final String RESPONSE_SUFFIX = "_response";
    private static final String DEFINITIONS = "definitions";
    private final Generator generator;

    public SowaSchemaGeneratorImpl(Generator generator) {
        this.generator = generator;
    }

    @Override
    public List<SowaSchema> generateSchema(List<ClassMethod> restMethods) {
        if (CollectionUtils.isEmpty(restMethods)) {
            return List.of();
        }

        try {
            return restMethods
                    .stream()
                    .map(this::generate)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время генерации схемы из " + restMethods, e);
        }
    }

    private SowaSchema generate(ClassMethod restMethod) {
        var restName = restMethod.restControllerMethodName();
        var request = generate(restMethod.request(), restName.concat(REQUEST_SUFFIX));
        var response = generate(restMethod.response(), restName.concat(RESPONSE_SUFFIX));
        return new SowaSchema(request, response, restName, restMethod.httpMethod());
    }

    private GeneratedResult generate(ResolvedType type, String schemaName) {
        if (type == null) {
            return null;
        }

        var generatedSchema = generator.getSchemaGenerator().generateSchema(type);
        var definitions = extractDefinitions(generatedSchema);
        deleteDefinitions(generatedSchema);
        return new GeneratedResult(schemaName, generatedSchema, definitions);
    }

    private List<GeneratedResult> extractDefinitions(ObjectNode generatedSchema) {
        var definitions = generatedSchema.get(DEFINITIONS);
        if (definitions == null) {
            return List.of();
        }

        var definitionList = new ArrayList<GeneratedResult>();
        for (var node : new NodeIterable(definitions.fields())) {
            var schema = new GeneratedResult(node.getKey(), (ObjectNode) node.getValue(), null);
            definitionList.add(schema);
        }
        return definitionList;
    }

    private void deleteDefinitions(ObjectNode schema) {
        schema.remove(DEFINITIONS);
    }

    private record NodeIterable(Iterator<Map.Entry<String, JsonNode>> iterator)
            implements Iterable<Map.Entry<String, JsonNode>> {
    }
}
