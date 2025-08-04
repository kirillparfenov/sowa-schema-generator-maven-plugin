package dev.parfenov.sowa.schema.plugin.generators.config;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.impl.SchemaCleanUpUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Провайдер кастомных определений для генерации JSON схем типов Map.
 * <p>
 * Класс обеспечивает специализированную обработку Map типов в зависимости от типа ключа:
 * <ul>
 *   <li>Для Map с ключами не являющимися Enum - создает общую объектную схему</li>
 *   <li>Для Map с ключами типа Enum - создает строго типизированную схему со свойствами,
 *       соответствующими значениям перечисления</li>
 * </ul>
 *
 * <p>
 * Реализует два интерфейса:
 * <ul>
 *   <li>{@link CustomDefinitionProviderV2} - для общей обработки типов Map</li>
 *   <li>{@link CustomPropertyDefinitionProvider} - для специализированной обработки
 *       полей типа Map с Enum ключами</li>
 * </ul>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class CustomMapDefinitionProvider implements CustomDefinitionProviderV2, CustomPropertyDefinitionProvider<FieldScope> {

    /**
     * Предоставляет кастомное определение схемы для Map типов.
     * <p>
     * Этот метод вызывается для обработки общих Map типов независимо от типа ключа.
     * Создает inline определение с общей объектной схемой, которая не включает
     * дополнительные атрибуты.
     *
     * @param targetType целевой тип для которого создается схема
     * @param context    контекст генерации схемы, содержащий конфигурацию и утилиты
     * @return кастомное определение схемы для Map или null, если тип не поддерживается
     */
    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType targetType, SchemaGenerationContext context) {
        var key = context.getTypeContext().getTypeParameterFor(targetType, Map.class, 0);
        if (key == null) {
            return null;
        }
        var schema = anySchema(context);
        appendAdditionalProperties(schema, targetType, context);
        return new CustomDefinition(schema, CustomDefinition.DefinitionType.INLINE, CustomDefinition.AttributeInclusion.NO);
    }

    /**
     * Создает общую объектную схему для Map с ключами не являющимися Enum.
     * <p>
     * Формирует базовую JSON схему типа "object", которая не накладывает
     * ограничений на структуру свойств Map. Используется для случаев,
     * когда тип ключа не позволяет определить конкретную структуру.
     *
     * @param context контекст генерации схемы для получения конфигурации
     * @return ObjectNode представляющий общую объектную схему
     */
    private ObjectNode anySchema(SchemaGenerationContext context) {
        var customSchema = context.getGeneratorConfig().createObjectNode();
        var objectNode = context.getGeneratorConfig().createObjectNode().textNode(context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));
        customSchema.set(context.getKeyword(SchemaKeyword.TAG_TYPE), objectNode);
        return customSchema;
    }

    private void appendAdditionalProperties(ObjectNode main, ResolvedType targetType, SchemaGenerationContext context) {
        var additionalProperties = context.createDefinition(
                context.getTypeContext().getTypeParameterFor(targetType, Map.class, 1)
        );
        main.set(context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES), additionalProperties);
    }

    /**
     * Предоставляет кастомное определение свойства для полей типа {@code Map<? extends Enum, T>}.
     * <p>
     * Этот метод специализируется на обработке Map полей, где ключом является Enum тип.
     * Создает строго типизированную схему, где каждое значение перечисления становится
     * именованным свойством объекта. Для Map с не-Enum ключами возвращает null.
     *
     * @param field   поле класса, представляющее Map с потенциально Enum ключами
     * @param context контекст генерации схемы для доступа к конфигурации и утилитам
     * @return кастомное определение свойства для Enum-ключевых Map или null для других типов
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope field, SchemaGenerationContext context) {
        var key = context.getTypeContext().getTypeParameterFor(field.getType(), Map.class, 0);

        if (key == null || !key.isInstanceOf(Enum.class)) return null;

        var enumSet = EnumSet.allOf((Class<? extends Enum>) key.getErasedType());
        return new CustomPropertyDefinition(
                enumSchema(enumSet, field, context),
                CustomDefinition.AttributeInclusion.NO
        );
    }

    /**
     * Создает специализированную схему для Map с Enum ключами.
     * <p>
     * Формирует JSON схему объектного типа, где каждое значение перечисления
     * становится именованным свойством. Устанавливает {@code additionalProperties: false}
     * для строгой типизации. Все свойства имеют одинаковый тип, определяемый
     * типом значения Map.
     *
     * @param enumValues множество значений перечисления для создания свойств
     * @param field      исходное поле Map для определения типа значений
     * @param context    контекст генерации схемы
     * @return ObjectNode представляющий строго типизированную схему объекта
     */
    private ObjectNode enumSchema(EnumSet<?> enumValues, FieldScope field, SchemaGenerationContext context) {
        var customSchema = anySchema(context);
        var propertiesNode = context.getGeneratorConfig().createObjectNode();

        customSchema.set(context.getKeyword(SchemaKeyword.TAG_PROPERTIES), propertiesNode);
        customSchema.put(context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES), false);

        var additionalProperties = extractAdditionalProperties(field, context);
        enumValues.stream()
                .map(Enum::name)
                .forEach(propertyName -> propertiesNode.set(propertyName, additionalProperties));

        return customSchema;
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
        var tagItems = context.getKeyword(SchemaKeyword.TAG_ITEMS);
        if (schema.has(additionalPropertyKey)) {
            var additionalProperties = schema.withObject(additionalPropertyKey);
            var items = additionalProperties.get(tagItems);
            if (items != null && items.isEmpty()) {
                var value = context.getTypeContext().getTypeParameterFor(field.getType(), Map.class, 1);
                var itemsNode = context.createDefinition(value).withObject(tagItems);
                additionalProperties.set(tagItems, itemsNode);
            }
        }
    }
}
