package dev.parfenov.sowa.schema.plugin.exporter.infra;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.parfenov.sowa.schema.plugin.parsers.classes.EndpointPathResolver;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClassMethod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.INFRASTRUCTURE_DIRECTORY;
import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.SOWA_DIRECTORY;

public class InfraExporterImpl implements InfraExporter {

    private static final String SCHEMA_PREFIX = "schemes/json/";
    private static final String RESPONSE_SUFFIX = "_response.json";
    private static final String REQUEST_SUFFIX = "_request.json";

    private final InfraConfig infraConfig;
    private final EndpointPathResolver endpointPathResolver;

    public InfraExporterImpl(final InfraConfig infraConfig) {
        this.infraConfig = infraConfig;
        this.endpointPathResolver = new EndpointPathResolver(infraConfig.project());
    }

    //todo привести в порядок этот бардак

    public ServicesYaml.RequestResponse buildRequest(RestClassMethod method, String idName) {
        if (method.getRequest() == null) return null;

        var requestResponse = new ServicesYaml.RequestResponse();
        requestResponse.setMethod(method.getHttpMethod().name().toLowerCase());
        requestResponse.setSchema(
                SCHEMA_PREFIX
                        .concat(infraConfig.sowaProfileName())
                        .concat("/request/")
                        .concat(idName)
                        .concat(REQUEST_SUFFIX)
        );
        requestResponse.setAllowEmptyBody(true);
        return requestResponse;
    }

    private ServicesYaml.RequestResponse buildResponse(RestClassMethod method, String idName) {
        if (method.getResponse() == null) return null;

        var response = new ServicesYaml.RequestResponse();
        response.setMethod(method.getHttpMethod().name().toLowerCase());
        response.setSchema(
                SCHEMA_PREFIX
                        .concat(infraConfig.sowaProfileName())
                        .concat("/response/")
                        .concat(idName)
                        .concat(RESPONSE_SUFFIX)
        );
        response.setAllowEmptyBody(true);
        var responseCode = new ServicesYaml.ResponseCode();
        responseCode.setOperator('~');
        responseCode.setPattern("^2\\d{2}$");
        response.setResponseCode(responseCode);
        return response;
    }

    private ServicesYaml.RequestResponse buildError400Response(RestClassMethod restClassMethod) {
        var response = new ServicesYaml.RequestResponse();
        response.setMethod(restClassMethod.getHttpMethod().name().toLowerCase());
        response.setSchema(
                SCHEMA_PREFIX
                        .concat(infraConfig.sowaProfileName())
                        .concat("/response/")
                        .concat("error_response_4XX.json")
        );
        response.setAllowEmptyBody(true);
        var responseCode = new ServicesYaml.ResponseCode();
        responseCode.setOperator('~');
        responseCode.setPattern("^4\\d{2}$");
        response.setResponseCode(responseCode);
        return response;
    }

    @Override
    public void export(List<RestClass> restClasses) {
        var servicesYaml = new ArrayList<ServicesYaml>();
        for (var restClass : restClasses) {
            for (var restMethod : restClass.getMethods()) {
                var serviceYaml = new ServicesYaml();
                var endpointToSchema = endpointPathResolver.endpointToSchema(restClass, restMethod);
                serviceYaml.setId(endpointToSchema);
                serviceYaml.setName(endpointToSchema);
                var fullPathWithRegex = endpointPathResolver.resolvePathWithVariables(restClass, restMethod);
                serviceYaml.setUrl(fullPathWithRegex);

                var allowedQuery = new ServicesYaml.AllowedQuery();
                if (restMethod.getHttpMethod() != null) {
                    allowedQuery.setMethod(restMethod.getHttpMethod().name().toLowerCase());
                }
                serviceYaml.setAllowedQueries(List.of(allowedQuery));

                var responses = new ArrayList<ServicesYaml.RequestResponse>();
                var requests = new ArrayList<ServicesYaml.RequestResponse>();

                var response = buildResponse(restMethod, endpointToSchema);
                if (response != null) {
                    responses.add(response);
                }
                var error4XXResponse = buildError400Response(restMethod);
                if (error4XXResponse != null) {
                    responses.add(error4XXResponse);
                }

                var request = buildRequest(restMethod, endpointToSchema);
                if (request != null) {
                    requests.add(request);
                }

                var jsonValidator = new ServicesYaml.ValidatorJson();
                if (!responses.isEmpty()) {
                    jsonValidator.setResponse(responses);
                }
                if (!requests.isEmpty()) {
                    jsonValidator.setRequest(requests);
                }

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
                yamlMapper.writeValue(servicesYamlFile, servicesYaml);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка во время экспорта services.yml", e);
            }
        }
    }
}
