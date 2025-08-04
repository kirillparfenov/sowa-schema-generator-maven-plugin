/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporters.infra;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import dev.parfenov.sowa.schema.plugin.generators.NameGenerator;
import dev.parfenov.sowa.schema.plugin.parsers.EndpointPathParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.emitter.Emitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Экспортер инфраструктурных конфигураций SOWA.
 * Создает services.yml файл с настройками валидации схем для REST эндпоинтов.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class InfraExporter {

    private static final String PROXY_PREFIX = "^/proxy";
    private static final String SCHEMA_PREFIX = "schemes/json/";
    private static final String JSON_SUFFIX = ".json";
    private static final String REQUEST_PATH = "/request/";
    private static final String RESPONSE_PATH = "/response/";
    private static final String ERROR_RESPONSE_SCHEMA = "error_response_4XX.json";
    private static final String SUCCESS_2XX_PATTERN = "^2\\d{2}$";
    private static final String ERROR_4XX_PATTERN = "^4\\d{2}$";
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
     * @param classModels коллекция REST классов для экспорта
     * @throws InfraExportException если произошла ошибка при экспорте
     */
    public void export(List<ClassModel> classModels) {
        if (CollectionUtils.isEmpty(classModels)) {
            return;
        }

        var servicesYaml = createServicesYaml(classModels);
        var filtered = canExportFilter(servicesYaml);
        filterById(filtered);
        exportYamlFile(filtered);
        postProcess();
    }

    /**
     * Создает коллекцию ServicesYaml из REST классов.
     *
     * @param classModels коллекция REST классов
     * @return список конфигураций сервисов
     */
    private List<ServicesYaml> createServicesYaml(List<ClassModel> classModels) {
        var services = classModels.stream()
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
     * @param classModel REST класс
     * @param method     REST метод
     * @return конфигурация сервиса
     */
    private ServicesYaml createServiceYaml(ClassModel classModel, MethodModel method) {
        var schemaID = NameGenerator.schemaID(classModel, method);
        var fullPath = getFullPath(classModel, method);
        var canExport = method.getRequest().canExport() || method.getResponse().canExport();

        return servicesFactory.createService(
                canExport,
                schemaID,
                fullPath,
                getAllowedQueries(method),
                createValidator(classModel, method)
        );
    }

    /**
     * Создает валидатор для метода.
     *
     * @param classModel REST класс
     * @param method     REST метод
     * @return конфигурация валидатора
     */
    private ServicesYaml.Validator createValidator(ClassModel classModel, MethodModel method) {
        var requests = createRequests(classModel, method);
        var responses = createResponses(classModel, method);

        var jsonValidator = servicesFactory.createValidatorJson(requests, responses);
        return servicesFactory.createValidator(jsonValidator);
    }

    /**
     * Формирует блок request/response.
     *
     * @param classModel REST класс
     * @param method     REST метод
     * @return список конфигураций запросов
     */
    private List<ServicesYaml.RequestResponse> createRequests(ClassModel classModel, MethodModel method) {
        return Optional
                .ofNullable(createSuccessRequest(classModel, method))
                .map(List::of)
                .orElseGet(List::of);
    }

    /**
     * Создает блок с конфигурацией запроса
     *
     * @param classModel REST класс
     * @param method     REST метод
     * @return null, либо конфигурацию с запросом
     */
    private ServicesYaml.RequestResponse createSuccessRequest(ClassModel classModel, MethodModel method) {
        if (method.getRequest().getType() == null || HttpMethod.GET.equals(method.getHttpMethod())) return null;

        return servicesFactory.createRequestResponse(
                getHttpMethodName(method),
                buildRequestPath(classModel, method),
                null
        );
    }

    /**
     * Создает конфигурации ответов (включая успешные и ошибочные).
     *
     * @param classModel REST класс
     * @param method     REST метод
     * @return список конфигураций ответов
     */
    private List<ServicesYaml.RequestResponse> createResponses(ClassModel classModel, MethodModel method) {
        return Stream.of(
                        create2xxResponse(classModel, method),
                        create4xxResponse(method)
                )
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Создает блок с конфигурацией 2XX ответа
     *
     * @param classModel REST класс
     * @param method     REST метод
     * @return null, либо конфигурацию с успешным ответом
     */
    private ServicesYaml.RequestResponse create2xxResponse(ClassModel classModel, MethodModel method) {
        if (method.getResponse().getType() == null) return null;

        var httpMethod = getHttpMethodName(method);
        var schemaPath = buildResponsePath(classModel, method);
        var successCode = servicesFactory.createResponseCode(REGEX_OPERATOR, SUCCESS_2XX_PATTERN);

        return servicesFactory.createRequestResponse(
                httpMethod,
                schemaPath,
                successCode
        );
    }

    /**
     * Создает блок с конфигурацией 4XX ответа
     *
     * @param method REST метод
     * @return конфигурацию с 4XX ответом
     */
    private ServicesYaml.RequestResponse create4xxResponse(MethodModel method) {
        var httpMethod = getHttpMethodName(method);
        var errorSchemaPath = buildSchemaPath(RESPONSE_PATH, ERROR_RESPONSE_SCHEMA);
        var errorCode = servicesFactory.createResponseCode(REGEX_OPERATOR, ERROR_4XX_PATTERN);

        return servicesFactory.createRequestResponse(
                httpMethod,
                errorSchemaPath,
                errorCode
        );
    }

    /**
     * Извлекает имя HTTP-метода и конвертирует его в String.lowerCase
     *
     * @param method метод контроллера
     * @return название HTTP метода в нижнем регистре
     */
    private String getHttpMethodName(MethodModel method) {
        return method.getHttpMethod().name().toLowerCase();
    }

    /**
     * Создает список разрешенных запросов для метода.
     *
     * @param method REST метод
     * @return список разрешенных запросов
     */
    private List<ServicesYaml.AllowedQuery> getAllowedQueries(MethodModel method) {
        var allowedQuery = servicesFactory.createAllowedQuery(
                method.getHttpMethod().name().toLowerCase()
        );
        return List.of(allowedQuery);
    }

    /**
     * Строит полный путь до request-схемы
     *
     * @param classModel REST класс
     * @param method     REST метод
     * @return полный путь до request-схемы
     */
    private String buildRequestPath(ClassModel classModel, MethodModel method) {
        var schemaName = NameGenerator.requestSchemaName(classModel, method);
        return buildSchemaPath(REQUEST_PATH, schemaName + JSON_SUFFIX);
    }

    /**
     * Строит полный путь до response-схемы
     *
     * @param classModel REST класс
     * @param method     REST метод
     * @return полный путь до response-схемы
     */
    private String buildResponsePath(ClassModel classModel, MethodModel method) {
        var schemaName = NameGenerator.responseSchemaName(classModel, method);
        return buildSchemaPath(RESPONSE_PATH, schemaName + JSON_SUFFIX);
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
     * @param classModel REST класс
     * @param method     метод из REST класса
     * @return полный путь до REST-endpoint
     */
    private String getFullPath(ClassModel classModel, MethodModel method) {
        return PROXY_PREFIX + endpointPathParser.resolvePathWithVariables(classModel, method);
    }

    /**
     * Оставляет только те конфиги, у которых установлен признак {@link ServicesYaml#isExport()} в {@code true}
     *
     * @param servicesYaml конфигурации сервисов
     * @return отфильтрованные значения с признаком {@link ServicesYaml#isExport()} = {@code true}
     */
    private List<ServicesYaml> canExportFilter(List<ServicesYaml> servicesYaml) {
        return servicesYaml.stream()
                .filter(ServicesYaml::isExport)
                .collect(Collectors.toList());
    }

    /**
     * Сортировка по id
     *
     * @param servicesYaml конфигурации сервисов
     */
    private void filterById(List<ServicesYaml> servicesYaml) {
        servicesYaml.sort(Comparator.comparing(ServicesYaml::getId));
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
     * Удаляет одиночные кавычки вокруг '!include'.
     * Невозможно сделать это через наследование {@link YAMLMapper} или {@link Emitter}.
     */
    private void postProcess() {
        try {
            var yamlPath = directoriesBuilder.servicesYamlFile().toPath();
            var lines = Files.readAllLines(yamlPath, StandardCharsets.UTF_8);

            var modifiedLines = lines.stream()
                    .map(line -> line.replace("'!include'", "!include"))
                    .map(line -> line.replaceAll("/d\\+", "/\\\\d+"))
                    .map(line ->
                            line.contains("operator:") || line.contains("pattern:")
                                    ? line
                                    : line.replaceAll("\"", "")
                    )
                    .collect(Collectors.toList());

            Files.write(yamlPath, modifiedLines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при обработке '!include'");
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
