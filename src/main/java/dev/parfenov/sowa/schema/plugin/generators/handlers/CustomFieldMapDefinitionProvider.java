package dev.parfenov.sowa.schema.plugin.generators.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.impl.SchemaCleanUpUtils;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class CustomFieldMapDefinitionProvider extends MapDefinitionAbstract implements CustomPropertyDefinitionProvider<FieldScope> {

    /**
     * Предоставляет кастомное определение свойства для полей типа {@code Map<?, ?>}.
     *
     * @param field   поле класса, представляющее Map
     * @param context контекст генерации схемы для доступа к конфигурации и утилитам
     * @return кастомное определение свойства для Map или null для других типов
     */
    @Override
    public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope field, SchemaGenerationContext context) {
        var key = context.getTypeContext().getTypeParameterFor(field.getType(), Map.class, 0);
        if (key == null) {
            return null;
        }

        return new CustomPropertyDefinition(
                anyFieldSchema(field, context),
                CustomDefinition.AttributeInclusion.NO
        );
    }

    private ObjectNode anyFieldSchema(FieldScope field, SchemaGenerationContext context) {
        var fieldSchema = baseSchema(context);
        var additionalProperties = extractAdditionalProperties(field, context);
        appendPatternProperties(fieldSchema, additionalProperties, context);
        return fieldSchema;
    }

    /**
     * Извлекает схему типа значений Map для использования в качестве типа свойств.
     * <p>
     * Создает стандартное определение схемы для поля Map, применяет очистку
     * allOf узлов и извлекает схему дополнительных свойств. Результат используется
     * как тип для всех свойств в схеме Enum-ключевой Map.
     *
     * @param field   исходное поле Map для анализа типа значений
     * @param context контекст генерации схемы
     * @return JsonNode представляющий схему типа значений Map
     */
    private JsonNode extractAdditionalProperties(FieldScope field, SchemaGenerationContext context) {
        var schema = context.createStandardDefinition(field, this);
        new SchemaCleanUpUtils(context.getGeneratorConfig()).reduceAllOfNodes(List.of(schema));
        replaceItemsNode(schema, field, context);
        return schema.withObject(context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES));
    }

    private void replaceItemsNode(ObjectNode schema, FieldScope field, SchemaGenerationContext context) {
        //есть баг, когда приходят пустые items, то в итоговой схеме они так и остаются пустые, мб это из-за очистки выше
        //в любом случае для перестраховки нужно создать вручную definitions и взять оттуда items

        var additionalPropertyKey = context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES);
        if (schema.has(additionalPropertyKey)) {
            var additionalProperties = schema.withObject(additionalPropertyKey);
            var itemsKey = context.getKeyword(SchemaKeyword.TAG_ITEMS);
            var items = additionalProperties.get(itemsKey);
            if (items != null && items.isEmpty()) {
                var value = context.getTypeContext().getTypeParameterFor(field.getType(), Map.class, 1);
                var itemsNode = context.createDefinition(value).withObject(itemsKey);
                additionalProperties.set(itemsKey, itemsNode);
            }
        }
    }
}
