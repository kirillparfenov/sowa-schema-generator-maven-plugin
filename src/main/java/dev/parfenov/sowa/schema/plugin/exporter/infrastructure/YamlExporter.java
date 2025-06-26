package dev.parfenov.sowa.schema.plugin.exporter.infrastructure;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.parfenov.sowa.schema.plugin.parsers.classes.ClassMethod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.INFRASTRUCTURE_DIRECTORY;
import static dev.parfenov.sowa.schema.plugin.exporter.ExportDirectories.SOWA_DIRECTORY;

public class YamlExporter implements InfrastructureExporter {

    private final InfraConfig infraConfig;

    public YamlExporter(InfraConfig infraConfig) {
        this.infraConfig = infraConfig;
    }

    //todo привести в порядок этот бардак

    @Override
    public void export(List<ClassMethod> classMethods) {
        var servicesYaml = new ArrayList<ServicesYaml>();
        for (var classMethod : classMethods) {
            var serviceYaml = new ServicesYaml();
            serviceYaml.setId(classMethod.restControllerMethodName());
            serviceYaml.setName(classMethod.restControllerMethodName());
            serviceYaml.setUrl("^" + classMethod.endpointUrl());

            var allowedQuery = new ServicesYaml.AllowedQuery();
            allowedQuery.setMethod(classMethod.httpMethod().name().toLowerCase());
            serviceYaml.setAllowedQueries(List.of(allowedQuery));

            var validator = new ServicesYaml.Validator();
            var jsonValidator = new ServicesYaml.ValidatorJson();
            var response = new ServicesYaml.Request();
            response.setMethod(classMethod.httpMethod().name().toLowerCase());
            response.setSchema(
                    "schemes/json/"
                            .concat(infraConfig.sowaProfileName())
                            .concat("/response/")
                            .concat(classMethod.restControllerMethodName())
                            .concat("_response.json")
            );
            response.setAllowEmptyBody(true);
            var responseCode = new ServicesYaml.ResponseCode();
            responseCode.setOperator('=');
            responseCode.setPattern("200");
            response.setResponseCode(responseCode);
            jsonValidator.setResponse(List.of(response));
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
