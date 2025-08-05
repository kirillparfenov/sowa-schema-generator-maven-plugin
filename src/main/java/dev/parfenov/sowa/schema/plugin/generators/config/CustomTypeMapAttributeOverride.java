package dev.parfenov.sowa.schema.plugin.generators.config;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;

import java.util.Map;

/**
 * Класс для переопределения атрибутов Map типов в JSON схеме.
 * <p>
 * Данный класс реализует интерфейс {@link TypeAttributeOverrideV2} и расширяет {@link MapDefinitionAbstract}
 * для предоставления специфичной логики обработки Map типов в процессе генерации JSON схемы.
 * <p>
 * Основная функциональность:
 * <ul>
 *   <li>Определяет, является ли обрабатываемый тип Map</li>
 *   <li>Исключает обработку для member scope (поля классов)</li>
 *   <li>Устанавливает pattern properties для Map типов вместо additional properties</li>
 * </ul>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @see TypeAttributeOverrideV2
 * @see MapDefinitionAbstract
 * @since 2025-08-05
 */
public class CustomTypeMapAttributeOverride extends MapDefinitionAbstract implements TypeAttributeOverrideV2 {

    /**
     * Переопределяет атрибуты типов в JSON схеме для Map типов.
     * <p>
     * Метод выполняет следующие проверки и операции:
     * <ol>
     *   <li>Проверяет, что тип не равен null</li>
     *   <li>Проверяет, что тип является Map</li>
     *   <li>Проверяет, что scope не является MemberScope</li>
     *   <li>Устанавливает pattern properties для валидного Map типа</li>
     * </ol>
     *
     * @param schemaNode узел JSON схемы для модификации
     * @param scope      область видимости типа (TypeScope)
     * @param context    контекст генерации схемы, содержащий конфигурацию и метаданные
     */
    @Override
    public void overrideTypeAttributes(ObjectNode schemaNode, TypeScope scope, SchemaGenerationContext context) {
        var type = scope.getType();

        if (type == null) return;
        if (notMap(type, context)) return;
        if (isMemberScope(scope)) return;

        setPatternProperties(schemaNode, context);
    }

    /**
     * Проверяет, что переданный тип НЕ является Map.
     * <p>
     * Метод использует контекст типов для определения, имеет ли переданный тип
     * параметр типа для {@link Map} класса на позиции 0 (ключ Map).
     *
     * @param type    разрешенный тип для проверки
     * @param context контекст генерации схемы
     * @return {@code true} если тип НЕ является Map, {@code false} если является Map
     */
    private boolean notMap(ResolvedType type, SchemaGenerationContext context) {
        return context.getTypeContext().getTypeParameterFor(type, Map.class, 0) == null;
    }

    /**
     * Проверяет, является ли область видимости типа MemberScope.
     * <p>
     * MemberScope представляет поля классов или методы. Данный метод используется
     * для исключения обработки Map типов, когда они являются полями класса.
     *
     * @param typeScope область видимости типа для проверки
     * @return {@code true} если scope является MemberScope, {@code false} в противном случае
     */
    private boolean isMemberScope(TypeScope typeScope) {
        return typeScope instanceof MemberScope<?, ?>;
    }

    /**
     * Устанавливает pattern properties для Map типа в JSON схеме.
     * <p>
     * Метод выполняет следующие операции:
     * <ol>
     *   <li>Извлекает additional properties из схемы</li>
     *   <li>Удаляет additional properties из основной схемы</li>
     *   <li>Вызывает {@link #appendPatternProperties(ObjectNode, JsonNode, SchemaGenerationContext)}
     *       для установки pattern properties</li>
     * </ol>
     *
     * @param schemaNode узел JSON схемы для модификации
     * @param context    контекст генерации схемы
     */
    private void setPatternProperties(ObjectNode schemaNode, SchemaGenerationContext context) {
        var additionalPropertyTag = context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES);
        var patternPropertiesBody = schemaNode.remove(additionalPropertyTag);
        appendPatternProperties(schemaNode, patternPropertiesBody, context);
    }
}
