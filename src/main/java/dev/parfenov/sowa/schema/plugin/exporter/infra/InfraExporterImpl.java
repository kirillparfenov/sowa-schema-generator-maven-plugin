package dev.parfenov.sowa.schema.plugin.exporter.infra;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.parfenov.sowa.schema.plugin.parsers.classes.ClassMethod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.INFRASTRUCTURE_DIRECTORY;
import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.SOWA_DIRECTORY;

public class InfraExporterImpl implements InfraExporter {

    private final InfraConfig infraConfig;

    public InfraExporterImpl(InfraConfig infraConfig) {
        this.infraConfig = infraConfig;
    }

    //todo привести в порядок этот бардак

    public ServicesYaml.RequestResponse buildRequest(ClassMethod classMethod) {
        if (classMethod.request() == null) return null;

        var requestResponse = new ServicesYaml.RequestResponse();
        requestResponse.setMethod(classMethod.httpMethod().name().toLowerCase());
        requestResponse.setSchema(
                "schemes/json/"
                        .concat(infraConfig.sowaProfileName())
                        .concat("/request/")
                        .concat(classMethod.endpointName())
                        .concat("_request.json")
        );
        requestResponse.setAllowEmptyBody(true);
        return requestResponse;
    }

    private ServicesYaml.RequestResponse buildResponse(ClassMethod classMethod) {
        if (classMethod.request() == null) return null;

        var response = new ServicesYaml.RequestResponse();
        response.setMethod(classMethod.httpMethod().name().toLowerCase());
        response.setSchema(
                "schemes/json/"
                        .concat(infraConfig.sowaProfileName())
                        .concat("/response/")
                        .concat(classMethod.endpointName())
                        .concat("_response.json")
        );
        response.setAllowEmptyBody(true);
        var responseCode = new ServicesYaml.ResponseCode();
        responseCode.setOperator('=');
        responseCode.setPattern("200");
        response.setResponseCode(responseCode);
        return response;
    }

    private ServicesYaml.RequestResponse buildError400Response(ClassMethod classMethod) {
        var response = new ServicesYaml.RequestResponse();
        response.setMethod(classMethod.httpMethod().name().toLowerCase());
        response.setSchema(
                "schemes/json/"
                        .concat(infraConfig.sowaProfileName())
                        .concat("/response/")
                        .concat("error_response_4XXX.json")
        );
        response.setAllowEmptyBody(true);
        var responseCode = new ServicesYaml.ResponseCode();
        responseCode.setOperator('~');
        responseCode.setPattern("^4\\d{2}$");
        response.setResponseCode(responseCode);
        return response;
    }

    @Override
    public void export(List<ClassMethod> classMethods) {
        var servicesYaml = new ArrayList<ServicesYaml>();
        for (var classMethod : classMethods) {
            var serviceYaml = new ServicesYaml();
            serviceYaml.setId(classMethod.endpointName());
            serviceYaml.setName(classMethod.endpointName());
            serviceYaml.setUrl("^" + classMethod.endpointUrl());

            var allowedQuery = new ServicesYaml.AllowedQuery();
            allowedQuery.setMethod(classMethod.httpMethod().name().toLowerCase());
            serviceYaml.setAllowedQueries(List.of(allowedQuery));

            var responses = new ArrayList<ServicesYaml.RequestResponse>();
            var requests = new ArrayList<ServicesYaml.RequestResponse>();

            var response = buildResponse(classMethod);
            if (response != null) {
                responses.add(response);
            }
            var error4XXResponse = buildError400Response(classMethod);
            if (error4XXResponse != null) {
                responses.add(error4XXResponse);
            }

            var request = buildRequest(classMethod);
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
