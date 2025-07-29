package dev.parfenov.sowa.schema.plugin.generators.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import com.github.victools.jsonschema.generator.TypeScope;

import java.util.ArrayList;
import java.util.List;

public class AllOfAttributeOverride implements TypeAttributeOverrideV2 {

    @Override
    public void overrideTypeAttributes(ObjectNode collectedTypeAttributes, TypeScope scope, SchemaGenerationContext context) {
        var allOfTag = context.getKeyword(SchemaKeyword.TAG_ALLOF);
        handleAllOfTags(collectedTypeAttributes, allOfTag);
    }

    private void handleAllOfTags(JsonNode node, String allOfTag) {
        node.properties().forEach(property -> {
            if (property.getValue().isObject() && property.getValue().has(allOfTag)) {
                for (var child : property.getValue()) {
                    var emptyNode = findEmptyNode(child);
                    if (emptyNode != null) {
                        var notEmptyNodes = new ArrayList<ObjectNode>();
                        findNotEmpty(child, notEmptyNodes);
                        notEmptyNodes.forEach(emptyNode::setAll);
                        ((ObjectNode) node).set(property.getKey(), emptyNode);
                    }
                }
            }

            if (property.getValue().isObject() || property.getValue().isArray()) {
                handleAllOfTags(property.getValue(), allOfTag);
            }
        });
    }

    private ObjectNode findEmptyNode(JsonNode node) {
        for (JsonNode child : node) {
            if (child.isObject()) {
                if (child.isEmpty()) {
                    return (ObjectNode) child;
                } else {
                    return findEmptyNode(child);
                }
            }
            if (child.isArray()) {
                ObjectNode found = findEmptyNode(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void findNotEmpty(JsonNode node, List<ObjectNode> notEmptyNodes) {
        for (JsonNode child : node) {
            if (child.isObject() && !child.isEmpty()) {
                notEmptyNodes.add((ObjectNode) child);
            }
            if (child.isArray()) {
                findNotEmpty(child, notEmptyNodes);
            }
        }
    }
}
