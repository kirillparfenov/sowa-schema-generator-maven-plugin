package dev.parfenov.sowa.schema.plugin.exporters.infra.factories;

import dev.parfenov.sowa.schema.plugin.exporters.infra.ServicesYaml;

/**
 * Фабрика для создания объектов схемы валидации.
 *
 * <p>Отвечает за создание объектов {@link ServicesYaml.ValidationSchema}, которые
 * определяют настройки валидации JSON данных на основе файлов схем.</p>
 *
 * <p>Все создаваемые схемы валидации имеют тип "file" и ссылаются на
 * соответствующие файлы JSON схем в файловой системе.</p>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-05
 */
public class ValidationSchemaFactory {

    /**
     * Создает объект схемы валидации для указанного пути.
     *
     * @param schemaPath путь к файлу JSON схемы валидации
     * @return объект схемы валидации с типом "file"
     */
    public ServicesYaml.ValidationSchema createValidationSchema(String schemaPath) {
        var validationSchema = new ServicesYaml.ValidationSchema();
        validationSchema.setType("file");
        validationSchema.setPath(schemaPath);
        return validationSchema;
    }
}
