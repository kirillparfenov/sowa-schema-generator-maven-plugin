package dev.parfenov.sowa.schema.plugin.generators.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import com.github.victools.jsonschema.generator.TypeScope;

/**
 * Переопределение атрибутов типов JSON схемы.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025
 */
public class CustomTypeAttributeOverride implements TypeAttributeOverrideV2 {

    /**
     * Переопределяет атрибуты типов в JSON схеме.
     *
     * @param schemaNode узел схемы для модификации
     * @param scope      область видимости типа
     * @param context    контекст генерации схемы
     */
    @Override
    public void overrideTypeAttributes(ObjectNode schemaNode, TypeScope scope, SchemaGenerationContext context) {
        var type = scope.getType();
        if (type != null && isByteType(type.getErasedType())) {
            addAdditionalArrayValue(schemaNode, SchemaKeyword.TAG_TYPE_INTEGER, context);
            removeArrayValue(schemaNode, SchemaKeyword.TAG_TYPE_STRING, context);
            addTextFieldIfAbsent(schemaNode, context.getKeyword(SchemaKeyword.TAG_FORMAT), byte.class.getName());
        }

        if (type != null && type.getErasedType().isEnum()) {
            repeatEnumLowerCase(schemaNode, context);
        }
    }

    /**
     * Проверяет, является ли тип байтовым (Byte или byte).
     *
     * @param erasedType тип для проверки
     * @return true, если тип является Byte.class или byte.class
     */
    private boolean isByteType(Class<?> erasedType) {
        return erasedType.equals(Byte.class) || erasedType.equals(byte.class);
    }

    /**
     * Добавляет дополнительный тип к массиву с типами.
     * Если поле с типами уже существует, преобразует его в массив (если оно одиночное)
     * и добавляет новый тип.
     *
     * @param schemaNode      узел схемы для модификации
     * @param additionalValue дополнительный тип для добавления
     * @param context         контекст генерации схемы
     */
    private void addAdditionalArrayValue(ObjectNode schemaNode, SchemaKeyword additionalValue, SchemaGenerationContext context) {
        final var typeKeyword = context.getKeyword(SchemaKeyword.TAG_TYPE);
        final var additionalTypeValue = context.getKeyword(additionalValue);

        if (!schemaNode.has(typeKeyword)) {
            initializeTypeArray(schemaNode, typeKeyword);
        }

        var jsonNode = schemaNode.get(typeKeyword);
        if (jsonNode.isTextual()) {
            convertTextualTypeToArray(schemaNode, typeKeyword, jsonNode.asText(), additionalTypeValue);
        } else if (jsonNode.isArray()) {
            addTypeToExistingArray(schemaNode, typeKeyword, additionalTypeValue);
        }
    }

    /**
     * Удаляет элемент массива.
     *
     * @param schemaNode  узел схемы для модификации
     * @param removeValue удаляемый тип из массива
     * @param context     контекст генерации схемы
     */
    private void removeArrayValue(ObjectNode schemaNode, SchemaKeyword removeValue, SchemaGenerationContext context) {
        final var typeKeyword = context.getKeyword(SchemaKeyword.TAG_TYPE);
        final var removingTypeValue = context.getKeyword(removeValue);

        if (!schemaNode.has(typeKeyword)) {
            initializeTypeArray(schemaNode, typeKeyword);
        }

        var jsonNode = schemaNode.get(typeKeyword);
        if (jsonNode.isTextual()) {
            var text = jsonNode.asText();
            schemaNode.putArray(typeKeyword).add(text);
        }
        schemaNode.withArray(typeKeyword).removeIf(node -> node.asText().equals(removingTypeValue));
    }

    /**
     * Инициализирует массив типов в узле схемы.
     *
     * @param schemaNode  узел схемы
     * @param typeKeyword название массива
     */
    private void initializeTypeArray(ObjectNode schemaNode, String typeKeyword) {
        schemaNode.putArray(typeKeyword);
    }

    /**
     * Преобразует текстовый тип в массив и добавляет дополнительный тип.
     *
     * @param schemaNode          узел схемы
     * @param arrayKeyword        название массива
     * @param existingType        существующий тип в текстовом формате
     * @param additionalTypeValue дополнительный тип для добавления
     */
    private void convertTextualTypeToArray(
            ObjectNode schemaNode,
            String arrayKeyword,
            String existingType,
            String additionalTypeValue
    ) {
        schemaNode.putArray(arrayKeyword)
                .add(existingType)
                .add(additionalTypeValue);
    }

    /**
     * Добавляет тип к существующему массиву типов.
     *
     * @param schemaNode          узел схемы
     * @param typeKeyword         ключевое слово типа
     * @param additionalTypeValue дополнительный тип для добавления
     */
    private void addTypeToExistingArray(ObjectNode schemaNode, String typeKeyword, String additionalTypeValue) {
        schemaNode.withArray(typeKeyword).add(additionalTypeValue);
    }

    /**
     * Добавляет текстовое поле в узел схемы, если оно отсутствует.
     * Проверяет наличие поля с указанным именем и добавляет его только в случае отсутствия.
     *
     * @param schemaNode узел схемы для модификации
     * @param fieldName  имя поля для добавления
     * @param fieldValue значение поля для добавления
     */
    private void addTextFieldIfAbsent(ObjectNode schemaNode, String fieldName, String fieldValue) {
        if (!schemaNode.has(fieldName)) {
            schemaNode.put(fieldName, fieldValue);
        }
    }

    /**
     * Дублирует значения enum в нижнем регистре в схеме JSON.
     * Берет существующий массив enum значений, преобразует их в нижний регистр
     * и добавляет эти варианты к исходному массиву для поддержки case-insensitive валидации.
     *
     * @param schemaNode узел схемы для модификации
     * @param context    контекст генерации схемы
     */
    private void repeatEnumLowerCase(ObjectNode schemaNode, SchemaGenerationContext context) {
        var enumNode = schemaNode.get(context.getKeyword(SchemaKeyword.TAG_ENUM));
        if (enumNode instanceof ArrayNode arrayNode) {
            arrayNode.valueStream()
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::asText)
                    .map(String::toLowerCase)
                    .toList()
                    .forEach(arrayNode::add);
        }
    }
}
