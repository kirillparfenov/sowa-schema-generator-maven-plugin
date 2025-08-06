package dev.parfenov.sowa.schema.plugin.exporters.infra.factories;

import dev.parfenov.sowa.schema.plugin.exporters.infra.Actions;
import dev.parfenov.sowa.schema.plugin.exporters.infra.InfraConfig;
import dev.parfenov.sowa.schema.plugin.exporters.infra.ServicesYaml;
import dev.parfenov.sowa.schema.plugin.generators.NameGenerator;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;

/**
 * Фабрика для создания параметров действий валидации.
 *
 * <p>Отвечает за создание объектов {@link ServicesYaml.Param}, которые содержат
 * параметры для различных типов действий валидации. Автоматически генерирует
 * пути к схемам валидации на основе структуры проекта и имен endpoint'ов.</p>
 *
 * <p>Поддерживаемые типы параметров:</p>
 * <ul>
 *   <li>Параметры для JSON валидации с путями к схемам</li>
 *   <li>Параметры для проверки размера данных</li>
 *   <li>Параметры для валидации ошибок 4xx</li>
 * </ul>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-05
 */
public class ParamFactory {

    private static final String JSON_SUFFIX = ".json";
    private static final String REQUEST_PATH = "/request/";
    private static final String SCHEMA_PREFIX = "schemes/json/";
    private static final String RESPONSE_PATH = "/response/";
    private static final String ERROR_RESPONSE_SCHEMA = "error_response_4XX.json";

    private final InfraConfig infraConfig;
    private final ValidationSchemaFactory validationSchemaFactory = new ValidationSchemaFactory();

    /**
     * Создает новый экземпляр фабрики параметров.
     *
     * @param infraConfig конфигурация инфраструктуры для генерации путей
     */
    public ParamFactory(InfraConfig infraConfig) {
        this.infraConfig = infraConfig;
    }

    /**
     * Создает параметр действия на основе пути к схеме и типа действия.
     *
     * @param schemaPath путь к файлу схемы валидации
     * @param actions    тип действия валидации
     * @return параметр с настройками для указанного действия
     */
    public ServicesYaml.Param createParam(String schemaPath, Actions actions) {
        var param = new ServicesYaml.Param();
        if (actions == Actions.JSON_VALIDATION) {
            param.setValidationSchema(validationSchemaFactory.createValidationSchema(schemaPath));
        } else if (actions == Actions.CHECK_DATA_SIZE) {
            param.setMaxAllowableSize("0");
        }
        return param;
    }

    /**
     * Создает параметр для валидации HTTP запроса.
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @param actions    тип действия валидации
     * @return параметр для валидации запроса
     */
    public ServicesYaml.Param createRequestParam(ClassModel classModel, MethodModel method, Actions actions) {
        return createParam(buildRequestPath(classModel, method), actions);
    }

    /**
     * Создает параметр для валидации HTTP ответа.
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @param actions    тип действия валидации
     * @return параметр для валидации ответа
     */
    public ServicesYaml.Param createResponseParam(ClassModel classModel, MethodModel method, Actions actions) {
        return createParam(buildResponsePath(classModel, method), actions);
    }

    /**
     * Создает параметр для валидации ошибок 4xx.
     *
     * @param actions тип действия валидации
     * @return параметр для валидации ошибок 4xx
     */
    public ServicesYaml.Param create4xxParam(Actions actions) {
        return createParam(buildSchemaPath(RESPONSE_PATH, ERROR_RESPONSE_SCHEMA), actions);
    }

    /**
     * Строит путь к схеме валидации для запроса.
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return путь к файлу схемы запроса
     */
    private String buildRequestPath(ClassModel classModel, MethodModel method) {
        var schemaName = NameGenerator.requestSchemaName(classModel, method);
        return buildSchemaPath(REQUEST_PATH, schemaName + JSON_SUFFIX);
    }

    /**
     * Строит путь к схеме валидации для ответа.
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return путь к файлу схемы ответа
     */
    private String buildResponsePath(ClassModel classModel, MethodModel method) {
        var schemaName = NameGenerator.responseSchemaName(classModel, method);
        return buildSchemaPath(RESPONSE_PATH, schemaName + JSON_SUFFIX);
    }

    /**
     * Строит полный путь к файлу схемы.
     *
     * @param destination тип назначения (например, "/request/" или "/response/")
     * @param fileName    имя файла схемы
     * @return полный путь к файлу схемы
     */
    private String buildSchemaPath(String destination, String fileName) {
        return SCHEMA_PREFIX + infraConfig.sowaProfileName() + destination + fileName;
    }
}
