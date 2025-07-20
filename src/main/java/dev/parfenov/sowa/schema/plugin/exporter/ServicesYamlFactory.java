/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporter;

import dev.parfenov.sowa.schema.plugin.exporter.dto.ServicesYaml;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Фабрика для создания объектов ServicesYaml и связанных с ними компонентов.
 * Инкапсулирует логику создания конфигурационных объектов для services.yml файла.
 */
public class ServicesYamlFactory {

    /**
     * Создает основной объект конфигурации сервиса.
     *
     * @param id             идентификатор сервиса
     * @param url            URL сервиса с паттернами переменных
     * @param allowedQueries список разрешенных запросов
     * @param validator      конфигурация валидатора
     * @return настроенный объект ServicesYaml
     */
    public ServicesYaml createService(String id, String url,
                                      List<ServicesYaml.AllowedQuery> allowedQueries,
                                      ServicesYaml.Validator validator) {
        var serviceYaml = new ServicesYaml();
        serviceYaml.setId(id);
        serviceYaml.setName(id);
        serviceYaml.setUrl(url);
        serviceYaml.setAllowedQueries(allowedQueries);
        serviceYaml.setValidators(validator);
        return serviceYaml;
    }

    /**
     * Создает конфигурацию валидатора.
     *
     * @param validatorJson JSON валидатор с настройками для запросов и ответов
     * @return конфигурация валидатора
     */
    public ServicesYaml.Validator createValidator(ServicesYaml.ValidatorJson validatorJson) {
        var validator = new ServicesYaml.Validator();
        validator.setValidatorJson(validatorJson);
        return validator;
    }

    /**
     * Создает JSON валидатор с настройками для запросов и ответов.
     *
     * @param requests  список конфигураций для валидации запросов
     * @param responses список конфигураций для валидации ответов
     * @return JSON валидатор
     */
    public ServicesYaml.ValidatorJson createValidatorJson(List<ServicesYaml.RequestResponse> requests,
                                                          List<ServicesYaml.RequestResponse> responses) {
        var validatorJson = new ServicesYaml.ValidatorJson();
        if (!CollectionUtils.isEmpty(requests)) {
            validatorJson.setRequest(requests);
        }
        if (!CollectionUtils.isEmpty(responses)) {
            validatorJson.setResponse(responses);
        }
        return validatorJson;
    }

    /**
     * Создает конфигурацию запроса/ответа с валидацией.
     *
     * @param method       HTTP метод (get, post, put, delete и т.д.)
     * @param schemaPath   путь к JSON схеме для валидации
     * @param responseCode код ответа (только для ответов, null для запросов)
     * @return конфигурация запроса/ответа
     */
    public ServicesYaml.RequestResponse createRequestResponse(String method,
                                                              String schemaPath,
                                                              ServicesYaml.ResponseCode responseCode) {
        var requestResponse = new ServicesYaml.RequestResponse();
        requestResponse.setMethod(method);
        requestResponse.setSchema(schemaPath);
        if (responseCode != null) {
            requestResponse.setResponseCode(responseCode);
        }
        return requestResponse;
    }

    /**
     * Создает код ответа с паттерном для проверки HTTP статусов.
     *
     * @param operator оператор сравнения ('~' для regex, '=' для точного совпадения)
     * @param pattern  паттерн для проверки (например, "^2\\d{2}$" для 2xx кодов)
     * @return конфигурация кода ответа
     */
    public ServicesYaml.ResponseCode createResponseCode(char operator, String pattern) {
        var responseCode = new ServicesYaml.ResponseCode();
        responseCode.setOperator(operator);
        responseCode.setPattern(pattern);
        return responseCode;
    }

    /**
     * Создает разрешенный запрос для определенного HTTP метода.
     *
     * @param method HTTP метод в нижнем регистре (get, post, put, delete и т.д.)
     * @return конфигурация разрешенного запроса
     */
    public ServicesYaml.AllowedQuery createAllowedQuery(String method) {
        var allowedQuery = new ServicesYaml.AllowedQuery();
        allowedQuery.setMethod(method);
        return allowedQuery;
    }
} 