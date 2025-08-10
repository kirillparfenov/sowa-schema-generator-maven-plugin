/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporters.infra;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.parfenov.sowa.schema.plugin.config.InfraConfig;
import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import dev.parfenov.sowa.schema.plugin.exporters.infra.factories.ServicesYamlFactory;
import dev.parfenov.sowa.schema.plugin.generators.NameGenerator;
import dev.parfenov.sowa.schema.plugin.parsers.EndpointPathParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.emitter.Emitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Экспортер инфраструктурных конфигураций SOWA.
 * Создает services.yml файл с настройками валидации схем для REST эндпоинтов.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class InfraExporter {

    private static final String PROXY_PREFIX = "^/proxy";

    private final EndpointPathParser endpointPathParser;
    private final DirectoriesBuilder directoriesBuilder;
    private final ServicesYamlFactory servicesFactory;

    public InfraExporter(final InfraConfig infraConfig) {
        this.endpointPathParser = new EndpointPathParser(infraConfig.contextPath());
        this.directoriesBuilder = infraConfig.directoriesBuilder();
        this.servicesFactory = new ServicesYamlFactory(infraConfig);
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

        var servicesYml = groupByUrl(new GroupBy(servicesFactory), createServicesYaml(classModels));
        var filtered = canExportFilter(servicesYml);
        append4xxResponse(filtered);
        filterById(filtered);
        exportYamlFile(filtered);
        cleanQuotes();
    }

    /**
     * Создает коллекцию ServicesYaml из REST классов.
     *
     * @param classModels коллекция REST классов
     * @return список конфигураций сервисов
     */
    private List<ServicesYaml> createServicesYaml(List<ClassModel> classModels) {
        return classModels.stream()
                .filter(Objects::nonNull)
                .flatMap(restClass ->
                        restClass.getMethods()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(method -> createServiceYaml(restClass, method))
                )
                .collect(Collectors.toList());
    }

    private List<ServicesYaml> groupByUrl(GroupBy groupBy, List<ServicesYaml> services) {
        return groupBy.url(services);
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
                classModel, method
        );
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
     * Прицепить обработку 4xx ответа
     * */
    private void append4xxResponse(List<ServicesYaml> servicesYaml) {
        servicesFactory.append4xxResponse(servicesYaml);
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
    private void cleanQuotes() {
        try {
            var yamlPath = directoriesBuilder.servicesYamlFile().toPath();
            var lines = Files.readAllLines(yamlPath, StandardCharsets.UTF_8);

            var modifiedLines = lines.stream()
                    .map(line -> line.replace("'!include'", "!include"))
                    .map(line -> line.replaceAll("/d\\+", "/\\\\d+"))
                    .map(line ->
                            line.contains("val:") || line.contains("operator:") || line.contains("max_allowable_size:")
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
