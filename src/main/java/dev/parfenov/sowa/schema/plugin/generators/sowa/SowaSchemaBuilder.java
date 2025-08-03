package dev.parfenov.sowa.schema.plugin.generators.sowa;

import dev.parfenov.sowa.schema.plugin.generators.Generator;
import dev.parfenov.sowa.schema.plugin.generators.NameGenerator;
import dev.parfenov.sowa.schema.plugin.generators.dto.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.Entity;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Построитель схем Sowa из REST классов.
 * <p>
 * Преобразует информацию о REST классах и их методах в схемы Sowa,
 * генерируя JSON Schema для типов запросов и ответов.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class SowaSchemaBuilder {

    private final Generator generator;

    public SowaSchemaBuilder(final Generator generator) {
        this.generator = generator;
    }

    /**
     * Генерирует схемы Sowa для списка REST классов.
     *
     * @param classModels список REST контроллеров для обработки
     */
    public void setSowaSchemas(List<ClassModel> classModels) {
        if (CollectionUtils.isEmpty(classModels)) {
            return;
        }
        try {
            classModels.forEach(this::generate);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время генерации схемы из " + classModels, e);
        }
    }

    /**
     * Генерирует схемы для одного REST класса.
     *
     * @param classModel REST контроллер для обработки
     */
    private void generate(ClassModel classModel) {
        for (var method : classModel.getMethods()) {
            setRequest(classModel, method);
            setResponse(classModel, method);
        }
    }

    /**
     * Установить схему запроса {@link Entity#setSchema(GeneratedResult)}
     *
     * @param classModel REST контроллер
     * @param method     REST метод
     */
    private void setRequest(ClassModel classModel, MethodModel method) {
        var schemaName = NameGenerator.requestSchemaName(classModel, method);
        var type = method.getRequest().getType();
        method.getRequest().setSchema(generate(type, schemaName));
    }

    /**
     * Установить схему ответа {@link Entity#setSchema(GeneratedResult)}
     *
     * @param classModel REST контроллер
     * @param method     REST метод
     */
    private void setResponse(ClassModel classModel, MethodModel method) {
        var schemaName = NameGenerator.responseSchemaName(classModel, method);
        var type = method.getResponse().getType();
        method.getResponse().setSchema(generate(type, schemaName));
    }

    /**
     * Генерирует JSON Schema для указанного типа.
     *
     * @param type       тип для генерации схемы
     * @param schemaName имя схемы
     * @return результат генерации или null если тип не указан
     */
    private GeneratedResult generate(Type type, String schemaName) {
        return type == null ? null : generator.generate(type, schemaName);
    }
}
