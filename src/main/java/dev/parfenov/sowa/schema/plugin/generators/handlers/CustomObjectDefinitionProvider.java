package dev.parfenov.sowa.schema.plugin.generators.handlers;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

/**
 * Провайдер кастомных определений для базового типа Object.
 * <p>
 * Этот класс предоставляет специальную схему JSON Schema для типа {@link Object},
 * которая позволяет дополнительные свойства и поддерживает как объектный тип,
 * так и null значения.
 * </p>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @see CustomDefinitionProviderV2
 * @since 2025-08-03
 */
public class CustomObjectDefinitionProvider implements CustomDefinitionProviderV2 {

    /**
     * Предоставляет кастомное определение схемы для указанного Java типа.
     * <p>
     * Метод проверяет, является ли переданный тип базовым классом {@link Object},
     * и если да, то возвращает специальное определение схемы для него.
     * </p>
     *
     * @param javaType тип Java, для которого нужно предоставить определение схемы
     * @param context  контекст генерации схемы
     * @return кастомное определение схемы для типа Object или null, если тип не Object
     */
    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
        if (!javaType.getErasedType().equals(Object.class)) return null;
        return buildCustomDefinition(context);
    }

    /**
     * Строит кастомное определение схемы для типа {@link Object}.
     * <p>
     * Создает встроенное определение с включением всех атрибутов,
     * используя конфигурацию по умолчанию для объектов.
     * </p>
     *
     * @param context контекст генерации схемы
     * @return кастомное определение схемы
     */
    private CustomDefinition buildCustomDefinition(SchemaGenerationContext context) {
        var node = buildCustomNode(context);
        return new CustomDefinition(node, CustomDefinition.INLINE_DEFINITION, CustomDefinition.AttributeInclusion.YES);
    }

    /**
     * Устанавливает определение объекта по умолчанию.
     * <p>
     * Создает JSON объект со следующими свойствами:
     * <ul>
     * <li>{@code additionalProperties: true}</li>
     * <li>{@code type: [object, null]}</li>
     * </ul>
     * </p>
     *
     * @param context контекст генерации схемы
     * @return узел ObjectNode с определением схемы
     */
    private ObjectNode buildCustomNode(SchemaGenerationContext context) {
        var node = context.getGeneratorConfig().createObjectNode();
        node.set(context.getKeyword(SchemaKeyword.TAG_TYPE), availableTypes(context));
        node.put(context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES), true);
        return node;
    }

    /**
     * Создает массив доступных типов для схемы Object.
     * <p>
     * Возвращает массив, содержащий два типа:
     * <ul>
     * <li>{@code object} - для объектных значений</li>
     * <li>{@code null} - для null значений</li>
     * </ul>
     * </p>
     *
     * @param context контекст генерации схемы
     * @return {@link ArrayNode} с доступными типами
     */
    private ArrayNode availableTypes(SchemaGenerationContext context) {
        var arrayNode = context.getGeneratorConfig().createArrayNode();
        arrayNode.add(context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));
        arrayNode.add(context.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
        return arrayNode;
    }
}
