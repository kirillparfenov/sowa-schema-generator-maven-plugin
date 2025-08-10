package dev.parfenov.sowa.schema.plugin.generators.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import static dev.parfenov.sowa.schema.plugin.generators.handlers.ValidationConstants.MAX_PROPERTIES;
import static dev.parfenov.sowa.schema.plugin.generators.handlers.ValidationConstants.PATTERN_PROPERTIES_KEY;

/**
 * Абстрактный базовый класс для работы с определениями Map типов в JSON схемах.
 * <p>
 * Предоставляет общую функциональность для создания и настройки JSON схем для Map типов,
 * включая установку pattern properties и ограничений на дополнительные свойства.
 * <p>
 * Основные возможности:
 * <ul>
 *   <li>Создание базовой объектной схемы</li>
 *   <li>Установка pattern properties с регулярными выражениями для ключей</li>
 *   <li>Настройка ограничений на количество свойств и дополнительные свойства</li>
 * </ul>
 * <p>
 * Используется как базовый класс для специализированных реализаций обработки Map типов
 * в процессе генерации JSON схем.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @see CustomTypeMapAttributeOverride
 * @see CustomFieldMapDefinitionProvider
 * @since 2025-08-05
 */
public class MapDefinitionAbstract {

    /**
     * Создает базовую объектную схему для Map типов.
     * <p>
     * Метод создает новый {@link ObjectNode} с установленным типом "object"
     * в соответствии с JSON Schema спецификацией. Данная схема служит основой
     * для дальнейшего добавления pattern properties и других ограничений.
     *
     * @param context контекст генерации схемы, содержащий конфигурацию генератора
     *                и ключевые слова схемы
     * @return {@link ObjectNode} представляющий базовую объектную схему с типом "object"
     */
    protected ObjectNode baseSchema(SchemaGenerationContext context) {
        var customSchema = context.getGeneratorConfig().createObjectNode();
        customSchema.put(
                context.getKeyword(SchemaKeyword.TAG_TYPE),
                context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT)
        );
        return customSchema;
    }

    /**
     * Добавляет pattern properties к базовой схеме для валидации ключей Map.
     * <p>
     * Метод настраивает JSON схему для корректной обработки Map объектов с помощью:
     * <ul>
     *   <li><strong>Pattern Properties</strong> - регулярное выражение {@link ValidationConstants#PATTERN_PROPERTIES_KEY}
     *       для валидации ключей Map (допускает латинские и кириллические буквы, цифры и подчеркивания,
     *       длина от 1 до 255 символов)</li>
     *   <li><strong>Max Properties</strong> - ограничение максимального количества свойств до {@link ValidationConstants#MAX_PROPERTIES}</li>
     *   <li><strong>Additional Properties</strong> - запрет дополнительных свойств (false)</li>
     * </ul>
     * <p>
     * Данная конфигурация обеспечивает строгую валидацию Map структур в соответствии
     * с требованиями проекта и предотвращает добавление произвольных свойств.
     *
     * @param baseSchema            базовая схема {@link ObjectNode} для модификации
     * @param patternPropertiesBody тело pattern properties {@link JsonNode}, определяющее
     *                              схему для значений Map
     * @param context               контекст генерации схемы
     */
    protected void appendPatternProperties(ObjectNode baseSchema, JsonNode patternPropertiesBody, SchemaGenerationContext context) {
        var patternPropertiesSchema = context.getGeneratorConfig()
                .createObjectNode()
                .set(PATTERN_PROPERTIES_KEY, patternPropertiesBody);

        baseSchema.set(context.getKeyword(SchemaKeyword.TAG_PATTERN_PROPERTIES), patternPropertiesSchema);
        baseSchema.put(context.getKeyword(SchemaKeyword.TAG_PROPERTIES_MAX), MAX_PROPERTIES);
        baseSchema.put(context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES), false);
    }
}
