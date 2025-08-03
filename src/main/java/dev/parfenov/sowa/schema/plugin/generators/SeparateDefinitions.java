package dev.parfenov.sowa.schema.plugin.generators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import dev.parfenov.sowa.schema.plugin.generators.config.GeneratorConfig;
import dev.parfenov.sowa.schema.plugin.generators.dto.GeneratedResult;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Генератор JSON Schema с раздельными определениями.
 * <p>
 * Создает схемы где определения извлекаются в отдельные объекты,
 * а в основной схеме остаются только ссылки на них.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class SeparateDefinitions implements Generator {

    private static final String REF = "$ref";
    private static final String DEFINITIONS = "definitions";
    private final SchemaGenerator schemaGenerator;
    private final GeneratorConfig generatorConfig;

    /**
     * Создает генератор с конфигурацией.
     *
     * @param config конфигурация генератора
     */
    public SeparateDefinitions(GeneratorConfig config) {
        this.schemaGenerator = new SchemaGenerator(config.getConfig());
        this.generatorConfig = config;
    }

    /**
     * Генерирует JSON Schema для указанного типа с извлечением определений.
     *
     * @param type       тип для генерации схемы
     * @param schemaName имя схемы
     * @return результат генерации с основной схемой и списком определений
     */
    @Override
    public GeneratedResult generate(Type type, String schemaName) {
        var mainSchema = generateNode(type);
        var definitions = extractDefinitions(mainSchema);
        deleteDefinitions(mainSchema);
        return new GeneratedResult(schemaName, mainSchema, definitions);
    }

    /**
     * Генерирует узел схемы для типа и обновляет ссылки.
     *
     * @param type тип для генерации
     * @return узел ObjectNode со схемой
     */
    private ObjectNode generateNode(Type type) {
        var mainSchema = schemaGenerator.generateSchema(type);
        replaceRef(mainSchema);
        return mainSchema;
    }

    /**
     * Рекурсивно заменяет ссылки в схеме на новый формат.
     *
     * @param jsonNode узел для обработки
     */
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

    /**
     * Извлекает определения из основной схемы в отдельный список.
     *
     * @param mainSchema основная схема
     * @return список извлеченных определений
     */
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

    /**
     * Добавляет тег схемы к определению.
     *
     * @param node узел определения
     */
    private void setSchemaToDefinition(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            var schemaTag = generatorConfig.getConfig().getKeyword(SchemaKeyword.TAG_SCHEMA);
            var schemaVersion = generatorConfig.getConfig().getKeyword(SchemaKeyword.TAG_SCHEMA_VALUE);
            objectNode.set(schemaTag, new TextNode(schemaVersion));
        }
    }

    /**
     * Удаляет секцию определений из основной схемы.
     *
     * @param mainSchema основная схема
     */
    private void deleteDefinitions(ObjectNode mainSchema) {
        mainSchema.remove(DEFINITIONS);
    }

    /**
     * Итератор для обхода полей JSON узла.
     */
    private record NodeIterable(Iterator<Map.Entry<String, JsonNode>> iterator)
            implements Iterable<Map.Entry<String, JsonNode>> {
    }
}
