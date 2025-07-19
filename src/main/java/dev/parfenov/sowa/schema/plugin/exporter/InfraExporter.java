/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporter;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.parfenov.sowa.schema.plugin.exporter.dto.ServicesYaml;
import dev.parfenov.sowa.schema.plugin.parsers.EndpointPathParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Экспортер инфраструктурных конфигураций SOWA.
 * Создает services.yml файл с настройками валидации схем для REST эндпоинтов.
 */
public class InfraExporter {

    private static final String PROXY_PREFIX = "^/proxy";
    private static final String SCHEMA_PREFIX = "schemes/json/";
    private static final String EMPTY_RESPONSE = "empty_object.json";
    private static final String RESPONSE_SUFFIX = "_response.json";
    private static final String REQUEST_SUFFIX = "_request.json";
    private static final String REQUEST_PATH = "/request/";
    private static final String RESPONSE_PATH = "/response/";
    private static final String ERROR_RESPONSE_SCHEMA = "error_response_4XX.json";
    private static final String SUCCESS_CODE_PATTERN = "^2\\d{2}$";
    private static final String ERROR_CODE_PATTERN = "^4\\d{2}$";
    private static final char REGEX_OPERATOR = '~';

    private final InfraConfig infraConfig;
    private final EndpointPathParser endpointPathParser;
    private final DirectoriesBuilder directoriesBuilder;
    private final ServicesYamlFactory servicesFactory;

    public InfraExporter(final InfraConfig infraConfig) {
        this.infraConfig = infraConfig;
        this.endpointPathParser = new EndpointPathParser(infraConfig.project());
        this.directoriesBuilder = infraConfig.directoriesBuilder();
        this.servicesFactory = new ServicesYamlFactory();
    }

    /**
     * Экспортирует конфигурацию services.yml для REST классов.
     *
     * @param restClasses коллекция REST классов для экспорта
     * @throws InfraExportException если произошла ошибка при экспорте
     */
    public void export(List<RestClass> restClasses) {
        if (CollectionUtils.isEmpty(restClasses)) {
            return;
        }

        var servicesYaml = createServicesYaml(restClasses);
        exportYamlFile(servicesYaml);
    }

    /**
     * Создает коллекцию ServicesYaml из REST классов.
     *
     * @param restClasses коллекция REST классов
     * @return список конфигураций сервисов
     */
    private List<ServicesYaml> createServicesYaml(List<RestClass> restClasses) {
        var services = restClasses.stream()
                .filter(Objects::nonNull)
                .flatMap(restClass ->
                        restClass.getMethods()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(method -> createServiceYaml(restClass, method))
                )
                .collect(Collectors.toList());

        return new GroupBy(servicesFactory).url(services);
    }

    /**
     * Создает конфигурацию сервиса для одного метода.
     *
     * @param restClass  REST класс
     * @param restMethod REST метод
     * @return конфигурация сервиса
     */
    private ServicesYaml createServiceYaml(RestClass restClass, RestMethod restMethod) {
        var schemaName = getSchemaName(restClass, restMethod);
        var fullPath = getFullPath(restClass, restMethod);

        return servicesFactory.createService(
                schemaName,
                fullPath,
                getAllowedQueries(restMethod),
                createValidator(restMethod, schemaName)
        );
    }

    /**
     * Создает валидатор для метода.
     *
     * @param restMethod REST метод
     * @param schemaName имя схемы
     * @return конфигурация валидатора
     */
    private ServicesYaml.Validator createValidator(RestMethod restMethod, String schemaName) {
        var requests = createRequests(restMethod, schemaName);
        var responses = createResponses(restMethod, schemaName);

        var jsonValidator = servicesFactory.createValidatorJson(requests, responses);
        return servicesFactory.createValidator(jsonValidator);
    }

    /**
     * Формирует блок request/response.
     *
     * @param restMethod REST метод
     * @param schemaName имя схемы
     * @return список конфигураций запросов
     */
    private List<ServicesYaml.RequestResponse> createRequests(RestMethod restMethod, String schemaName) {
        return Optional
                .ofNullable(createSuccessRequest(restMethod, schemaName))
                .map(List::of)
                .orElseGet(List::of);
    }

    /**
     * Создает блок с конфигурацией запроса
     *
     * @param restMethod REST метод
     * @param schemaName имя схемы
     * @return null, либо конфигурацию с запросом
     */
    private ServicesYaml.RequestResponse createSuccessRequest(RestMethod restMethod, String schemaName) {
        if (restMethod.getRequest() == null) return null;

        var schemaPath = buildSchemaPath(REQUEST_PATH, schemaName + REQUEST_SUFFIX);

        return servicesFactory.createRequestResponse(
                getHttpMethodName(restMethod),
                schemaPath,
                null
        );
    }

    /**
     * Создает конфигурации ответов (включая успешные и ошибочные).
     *
     * @param restMethod REST метод
     * @param schemaName имя схемы
     * @return список конфигураций ответов
     */
    private List<ServicesYaml.RequestResponse> createResponses(RestMethod restMethod, String schemaName) {
        return Stream.of(
                        create2xxResponse(restMethod, schemaName),
                        create4xxResponse(restMethod)
                )
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Создает блок с конфигурацией 2XX ответа
     *
     * @param restMethod REST метод
     * @param schemaName имя схемы
     * @return null, либо конфигурацию с успешным ответом
     */
    private ServicesYaml.RequestResponse create2xxResponse(RestMethod restMethod, String schemaName) {
        if (restMethod.getResponse() == null) return null;

        var httpMethod = getHttpMethodName(restMethod);
        var fileName = buildResponseFilename(restMethod, schemaName);
        var schemaPath = buildSchemaPath(RESPONSE_PATH, fileName);
        var successCode = servicesFactory.createResponseCode(REGEX_OPERATOR, SUCCESS_CODE_PATTERN);

        return servicesFactory.createRequestResponse(
                httpMethod,
                schemaPath,
                successCode
        );
    }

    /**
     * Создает блок с конфигурацией 4XX ответа
     *
     * @param restMethod REST метод
     * @return конфигурацию с 4XX ответом
     */
    private ServicesYaml.RequestResponse create4xxResponse(RestMethod restMethod) {
        var httpMethod = getHttpMethodName(restMethod);
        var errorSchemaPath = buildSchemaPath(RESPONSE_PATH, ERROR_RESPONSE_SCHEMA);
        var errorCode = servicesFactory.createResponseCode(REGEX_OPERATOR, ERROR_CODE_PATTERN);

        return servicesFactory.createRequestResponse(
                httpMethod,
                errorSchemaPath,
                errorCode
        );
    }

    /**
     * Извлекает имя HTTP-метода и конвертирует его в String.lowerCase
     *
     * @param restMethod метод контроллера
     * @return название HTTP метода в нижнем регистре
     */
    private String getHttpMethodName(RestMethod restMethod) {
        return restMethod.getHttpMethod().name().toLowerCase();
    }

    /**
     * Проверка, что ответ метода void
     *
     * @param responseType тип ответа метода
     * @return true, если ответ метода является пустым
     */
    private boolean isVoidResponse(Type responseType) {
        return Optional
                .ofNullable(responseType)
                .map(void.class::equals)
                .orElse(false);
    }

    /**
     * Создает список разрешенных запросов для метода.
     *
     * @param method REST метод
     * @return список разрешенных запросов
     */
    private List<ServicesYaml.AllowedQuery> getAllowedQueries(RestMethod method) {
        var allowedQuery = servicesFactory.createAllowedQuery(
                method.getHttpMethod().name().toLowerCase()
        );
        return List.of(allowedQuery);
    }

    /**
     * Строит полный путь к схеме.
     *
     * @param destination папка назначения (/request/ или /response/)
     * @param fileName    имя файла схемы
     * @return полный путь к схеме
     */
    private String buildSchemaPath(String destination, String fileName) {
        return SCHEMA_PREFIX + infraConfig.sowaProfileName() + destination + fileName;
    }

    /**
     * Возвращает URL - полный путь до REST-endpoint
     *
     * @param restClass  REST класс
     * @param restMethod метод из REST класса
     * @return полный путь до REST-endpoint
     */
    private String getFullPath(RestClass restClass, RestMethod restMethod) {
        return PROXY_PREFIX + endpointPathParser.resolvePathWithVariables(restClass, restMethod);
    }

    /**
     * Возвращает имя файла с JSON-схемой
     *
     * @param restClass  REST класс
     * @param restMethod метод из REST класса
     * @return имя файла JSON-схемы
     */
    private String getSchemaName(RestClass restClass, RestMethod restMethod) {
        return isVoidResponse(restMethod.getResponse())
                ? EMPTY_RESPONSE
                : endpointPathParser.schemaName(restClass, restMethod);
    }

    /**
     * Строит имя response - схемы
     *
     * @param restMethod метод контроллера
     * @param schemaName имя схемы
     * @return имя файла response - схемы, либо empty_object.json при response == void
     */
    private String buildResponseFilename(RestMethod restMethod, String schemaName) {
        return isVoidResponse(restMethod.getResponse())
                ? schemaName
                : schemaName + RESPONSE_SUFFIX;
    }

    /**
     * Экспортирует services.yml файл.
     *
     * @param servicesYaml список конфигураций сервисов
     * @throws InfraExportException если произошла ошибка записи
     */
    private void exportYamlFile(List<ServicesYaml> servicesYaml) {
        try {
            createYamlMapper().writeValue(directoriesBuilder.servicesYamlFile(), servicesYaml);
        } catch (IOException e) {
            throw new InfraExportException("Ошибка записи файла services.yml", e);
        }
    }

    /**
     * Создает настроенный YAML маппер.
     *
     * @return настроенный YAMLMapper
     */
    private YAMLMapper createYamlMapper() {
        return new YAMLMapper()
                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                .configure(YAMLGenerator.Feature.ALLOW_LONG_KEYS, true)
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
    }

    /**
     * Исключение, выбрасываемое при ошибках экспорта инфраструктуры.
     */
    public static class InfraExportException extends RuntimeException {
        public InfraExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
