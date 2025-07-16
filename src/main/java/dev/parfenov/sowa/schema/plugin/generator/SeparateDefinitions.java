package dev.parfenov.sowa.schema.plugin.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SeparateDefinitions implements Generator {

    private static final String REF = "$ref";
    private static final String DEFINITIONS = "definitions";
    private final SchemaGenerator schemaGenerator;
    private final GeneratorConfig generatorConfig;

    public SeparateDefinitions(GeneratorConfig config) {
        this.schemaGenerator = new SchemaGenerator(config.getConfig());
        this.generatorConfig = config;
    }

    @Override
    public GeneratedResult generate(Type type, String schemaName) {
        var mainSchema = generateNode(type);
        var definitions = extractDefinitions(mainSchema);
        deleteDefinitions(mainSchema);
        return new GeneratedResult(schemaName, mainSchema, definitions);
    }

    private ObjectNode generateNode(Type type) {
        var mainSchema = schemaGenerator.generateSchema(type);
        replaceRef(mainSchema);
        return mainSchema;
    }

    private void replaceRef(JsonNode jsonNode) {
        if (jsonNode instanceof ObjectNode objectNode) {
            if (objectNode.has(REF)) {
                var oldRef = objectNode.get(REF).textValue();
                var newRef = GeneratorUtils.changeRefPath(oldRef);
                var textNodeRef = generatorConfig.getConfig().createObjectNode().textNode(newRef);
                objectNode.replace(REF, textNodeRef);
            }
        }

        for (var node : new NodeIterable(jsonNode.fields())) {
            replaceRef(node.getValue());
        }

        if (jsonNode instanceof ArrayNode arrayNode) {
            for (var array : arrayNode) {
                replaceRef(array);
            }
        }
    }

    private List<GeneratedResult> extractDefinitions(ObjectNode mainSchema) {
        var definitions = mainSchema.get(DEFINITIONS);
        if (definitions == null) {
            return List.of();
        }

        var definitionList = new ArrayList<GeneratedResult>();
        for (var node : new NodeIterable(definitions.fields())) {
            setSchemaToDefinition(node.getValue());
            var schema = new GeneratedResult(node.getKey(), (ObjectNode) node.getValue(), null);
            definitionList.add(schema);
        }
        return definitionList;
    }

    private void setSchemaToDefinition(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            var schemaTag = generatorConfig.getConfig().getKeyword(SchemaKeyword.TAG_SCHEMA);
            var schemaVersion = generatorConfig.getConfig().getKeyword(SchemaKeyword.TAG_SCHEMA_VALUE);
            objectNode.set(schemaTag, new TextNode(schemaVersion));
        }
    }

    private void deleteDefinitions(ObjectNode mainSchema) {
        mainSchema.remove(DEFINITIONS);
    }

    private record NodeIterable(Iterator<Map.Entry<String, JsonNode>> iterator)
            implements Iterable<Map.Entry<String, JsonNode>> {
    }
}
