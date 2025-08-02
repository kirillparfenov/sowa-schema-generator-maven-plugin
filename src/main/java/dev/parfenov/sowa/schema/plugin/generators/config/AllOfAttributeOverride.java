package dev.parfenov.sowa.schema.plugin.generators.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import com.github.victools.jsonschema.generator.TypeScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Переопределение атрибутов типов для обработки конструкций allOf в JSON схемах.
 * <p>
 * Этот класс реализует {@link TypeAttributeOverrideV2} для кастомной обработки
 * конструкций allOf, которые позволяют объединять несколько схем в одну.
 * Основная задача - найти пустые узлы в структуре allOf и заполнить их
 * содержимым из непустых узлов.
 * </p>
 * <p>
 * Пример использования:
 * <pre>{@code
 * {
 *   "allOf": [
 *     {},
 *     {"type": "string"},
 *     {"minLength": 5}
 *   ]
 * }
 * }</pre>
 * Будет преобразовано в схему, где пустой объект будет заполнен
 * свойствами из других элементов allOf.
 * </p>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class AllOfAttributeOverride implements TypeAttributeOverrideV2 {

    /**
     * Переопределяет атрибуты типа для обработки конструкций allOf.
     * <p>
     * Этот метод вызывается генератором схем для каждого типа и позволяет
     * модифицировать собранные атрибуты типа. В данной реализации происходит
     * поиск и обработка тегов allOf.
     * </p>
     *
     * @param collectedTypeAttributes собранные атрибуты типа (модифицируемый объект)
     * @param scope                   область видимости типа с информацией о классе/поле
     * @param context                 контекст генерации схемы с настройками и ключевыми словами
     * @throws NullPointerException если любой из параметров равен null
     */
    @Override
    public void overrideTypeAttributes(ObjectNode collectedTypeAttributes,
                                       TypeScope scope,
                                       SchemaGenerationContext context) {
        var allOfKeyword = context.getKeyword(SchemaKeyword.TAG_ALLOF);
        processAllOfTags(collectedTypeAttributes, allOfKeyword);
    }

    /**
     * Рекурсивно обрабатывает структуры allOf в JSON узле.
     * <p>
     * Метод проходит по всем свойствам узла и ищет конструкции allOf.
     * При нахождении такой конструкции вызывает метод для её обработки.
     * Также рекурсивно обрабатывает вложенные объекты и массивы.
     * </p>
     *
     * @param jsonNode     JSON узел для обработки
     * @param allOfKeyword ключевое слово allOf из контекста генерации
     */
    private void processAllOfTags(JsonNode jsonNode, String allOfKeyword) {
        jsonNode.properties().forEach(property -> {
            var propertyValue = property.getValue();
            var propertyKey = property.getKey();

            // Обработка конструкций allOf
            if (propertyValue.isObject() && propertyValue.has(allOfKeyword)) {
                var allOfArray = propertyValue.get(allOfKeyword);
                var mergedNode = mergeAllOfElements(allOfArray);
                if (mergedNode != null) {
                    ((ObjectNode) jsonNode).set(propertyKey, mergedNode);
                }
            }

            // Рекурсивная обработка вложенных структур
            if (propertyValue.isObject() || propertyValue.isArray()) {
                processAllOfTags(propertyValue, allOfKeyword);
            }
        });
    }

    /**
     * Объединяет элементы массива allOf, заполняя пустые узлы содержимым непустых.
     * <p>
     * Алгоритм работы:
     * 1. Находит первый пустой объект в массиве allOf
     * 2. Собирает все непустые объекты
     * 3. Объединяет содержимое непустых объектов в пустой
     * 4. Возвращает результирующий объект
     * </p>
     *
     * @param allOfArray массив элементов allOf
     * @return объединённый узел или null, если объединение невозможно
     */
    private ObjectNode mergeAllOfElements(JsonNode allOfArray) {
        if (allOfArray == null || !allOfArray.isArray()) {
            return null;
        }

        var emptyTargetNode = findFirstEmptyObject(allOfArray);
        if (emptyTargetNode == null) {
            return null;
        }

        var nonEmptyNodes = collectNonEmptyObjects(allOfArray);

        // Объединяем все непустые узлы в целевой пустой узел
        nonEmptyNodes.forEach(emptyTargetNode::setAll);

        return emptyTargetNode;
    }

    /**
     * Находит первый пустой объект в структуре JSON.
     * <p>
     * Пустым считается объект без свойств.
     * </p>
     *
     * @param jsonNode узел JSON для поиска
     * @return первый найденный пустой ObjectNode или null, если не найден
     */
    private ObjectNode findFirstEmptyObject(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }

        for (JsonNode childNode : jsonNode) {
            if (childNode.isObject() && childNode.isEmpty()) {
                return (ObjectNode) childNode;
            }
        }

        return null;
    }

    /**
     * Собирает все непустые объекты из структуры JSON.
     * <p>
     * Метод проходит по всем узлам и собирает непустые объекты
     * в список. Непустым считается объект, содержащий хотя бы одно свойство.
     * </p>
     *
     * @param jsonNode узел JSON для обхода
     * @return список всех найденных непустых ObjectNode
     */
    private List<ObjectNode> collectNonEmptyObjects(JsonNode jsonNode) {
        var nonEmptyObjects = new ArrayList<ObjectNode>();

        if (jsonNode == null) {
            return nonEmptyObjects;
        }

        for (JsonNode childNode : jsonNode) {
            if (!childNode.isEmpty()) {
                nonEmptyObjects.add((ObjectNode) childNode);
            }
        }

        return nonEmptyObjects;
    }
}
