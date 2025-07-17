package dev.parfenov.sowa.schema.plugin.exporter.infra;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.parfenov.sowa.schema.plugin.exporter.infra.dto.ServicesYaml;
import dev.parfenov.sowa.schema.plugin.parsers.EndpointPathParser;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClassMethod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.INFRASTRUCTURE_DIRECTORY;
import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.SOWA_DIRECTORY;

public class InfraExporter {

    private static final String SCHEMA_PREFIX = "schemes/json/";
    private static final String RESPONSE_SUFFIX = "_response.json";
    private static final String REQUEST_SUFFIX = "_request.json";

    private final InfraConfig infraConfig;
    private final EndpointPathParser endpointPathParser;

    public InfraExporter(final InfraConfig infraConfig) {
        this.infraConfig = infraConfig;
        this.endpointPathParser = new EndpointPathParser(infraConfig.project());
    }

    /**
     * Экспорт обвязки SOWA
     */
    public void export(List<RestClass> restClasses) {
        //todo нужно объединить RestClass по адресу до эндпоинта:
        // Map<String, List<RestClass>> - потому что на 1 адрес могут быть разные HTTP-методы
        var servicesYaml = new ArrayList<ServicesYaml>();
        for (var restClass : restClasses) {
            for (var restMethod : restClass.getMethods()) {
                var serviceYaml = new ServicesYaml();

                var endpointToSchema = endpointPathParser.endpointToSchema(restClass, restMethod);
                serviceYaml.setId(endpointToSchema);
                serviceYaml.setName(endpointToSchema);

                var fullPathWithRegex = endpointPathParser.resolvePathWithVariables(restClass, restMethod);
                serviceYaml.setUrl(fullPathWithRegex);

                serviceYaml.setAllowedQueries(getAllowedQueries(restMethod));

                var requests = getRequest(restMethod, endpointToSchema);
                var responses = getResponse(restMethod, endpointToSchema);
                var jsonValidator = getValidatorJson(requests, responses);
                var validator = new ServicesYaml.Validator();
                validator.setValidatorJson(jsonValidator);
                serviceYaml.setValidators(validator);

                servicesYaml.add(serviceYaml);
            }

            var sowaDir = new File(infraConfig.project().getBuild().getDirectory(), SOWA_DIRECTORY);
            var servicesDir = new File(sowaDir, INFRASTRUCTURE_DIRECTORY);
            servicesDir.mkdirs();
            var servicesYamlFile = new File(servicesDir, "services.yml");
            var yamlMapper = new YAMLMapper();
            try {
                yamlMapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
                yamlMapper.writeValue(servicesYamlFile, servicesYaml);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка во время экспорта services.yml", e);
            }
        }
    }

    private List<ServicesYaml.AllowedQuery> getAllowedQueries(RestClassMethod method) {
        var allowedQuery = new ServicesYaml.AllowedQuery();
        allowedQuery.setMethod(method.getHttpMethod().name().toLowerCase());
        return List.of(allowedQuery);
    }

    private List<ServicesYaml.RequestResponse> getResponse(RestClassMethod restMethod, String endpointToSchema) {
        var responses = new ArrayList<ServicesYaml.RequestResponse>();
        var response = buildResponse(restMethod, endpointToSchema);
        if (response != null) {
            responses.add(response);
        }
        responses.add(buildError400Response(restMethod));
        return responses;
    }

    private List<ServicesYaml.RequestResponse> getRequest(RestClassMethod restMethod, String endpointToSchema) {
        var requests = new ArrayList<ServicesYaml.RequestResponse>();
        var request = buildRequest(restMethod, endpointToSchema);
        if (request != null) {
            requests.add(request);
        }
        return requests;
    }

    public ServicesYaml.RequestResponse buildRequest(RestClassMethod method, String endpointToSchema) {
        if (method.getRequest() == null) return null;

        var requestResponse = new ServicesYaml.RequestResponse();
        requestResponse.setMethod(method.getHttpMethod().name().toLowerCase());
        requestResponse.setSchema(getSchemaName("/request/", endpointToSchema.concat(REQUEST_SUFFIX)));
        requestResponse.setAllowEmptyBody(true);
        return requestResponse;
    }

    private ServicesYaml.RequestResponse buildResponse(RestClassMethod method, String endpointToSchema) {
        if (method.getResponse() == null) return null;

        var response = new ServicesYaml.RequestResponse();
        response.setMethod(method.getHttpMethod().name().toLowerCase());
        response.setSchema(getSchemaName("/response/", endpointToSchema.concat(RESPONSE_SUFFIX)));
        response.setAllowEmptyBody(true);
        response.setResponseCode(buildCode('~', "^2\\d{2}$"));
        return response;
    }

    private ServicesYaml.RequestResponse buildError400Response(RestClassMethod restClassMethod) {
        var response = new ServicesYaml.RequestResponse();
        response.setMethod(restClassMethod.getHttpMethod().name().toLowerCase());
        response.setSchema(getSchemaName("/response/", "error_response_4XX.json"));
        response.setAllowEmptyBody(true);
        response.setResponseCode(buildCode('~', "^4\\d{2}$"));
        return response;
    }

    private ServicesYaml.ResponseCode buildCode(char operator, String pattern) {
        var responseCode = new ServicesYaml.ResponseCode();
        responseCode.setOperator(operator);
        responseCode.setPattern(pattern);
        return responseCode;
    }

    private ServicesYaml.ValidatorJson getValidatorJson(
            List<ServicesYaml.RequestResponse> requests,
            List<ServicesYaml.RequestResponse> responses
    ) {
        var jsonValidator = new ServicesYaml.ValidatorJson();
        if (!responses.isEmpty()) {
            jsonValidator.setResponse(responses);
        }
        if (!requests.isEmpty()) {
            jsonValidator.setRequest(requests);
        }
        return jsonValidator;
    }

    private String getSchemaName(String destination, String fileName) {
        return SCHEMA_PREFIX
                .concat(infraConfig.sowaProfileName())
                .concat(destination)
                .concat(fileName);
    }
}
