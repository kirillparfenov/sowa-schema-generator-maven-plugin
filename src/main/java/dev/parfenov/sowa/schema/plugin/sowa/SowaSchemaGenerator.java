package dev.parfenov.sowa.schema.plugin.sowa;

import dev.parfenov.sowa.schema.plugin.generator.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.generator.Generator;
import dev.parfenov.sowa.schema.plugin.parsers.classes.EndpointPathResolver;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClassMethod;
import org.apache.maven.project.MavenProject;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SowaSchemaGenerator {

    private static final String REQUEST_SUFFIX = "_request";
    private static final String RESPONSE_SUFFIX = "_response";

    private final Generator generator;
    private final EndpointPathResolver pathResolver;

    public SowaSchemaGenerator(final Generator generator,
                               final MavenProject mavenProject) {
        this.generator = generator;
        this.pathResolver = new EndpointPathResolver(mavenProject);
    }

    public List<SowaSchema> generateSchema(List<RestClass> restClasses) {
        if (CollectionUtils.isEmpty(restClasses)) {
            return List.of();
        }
        try {
            return restClasses
                    .stream()
                    .map(this::generate)
                    .flatMap(List::stream)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время генерации схемы из " + restClasses, e);
        }
    }

    private List<SowaSchema> generate(RestClass restClass) {
        var schemas = new ArrayList<SowaSchema>();
        for (var method : restClass.getMethods()) {
            var sowaSchema = new SowaSchema();
            setNames(sowaSchema, restClass, method);
            setRequestResponse(sowaSchema, restClass, method);
            setPath(sowaSchema, restClass, method);
            schemas.add(sowaSchema);
        }
        return schemas;
    }

    private void setNames(SowaSchema sowaSchema, RestClass restClass, RestClassMethod method) {
        sowaSchema.setRestClassName(restClass.getName());
        sowaSchema.setRestMethodName(method.getName());
    }

    private void setRequestResponse(SowaSchema sowaSchema, RestClass restClass, RestClassMethod method) {
        var endpointToSchema = pathResolver.endpointToSchema(restClass, method);
        var request = generate(method.getRequest(), endpointToSchema.concat(REQUEST_SUFFIX));
        var response = generate(method.getResponse(), endpointToSchema.concat(RESPONSE_SUFFIX));
        sowaSchema.setRequest(request);
        sowaSchema.setResponse(response);
    }

    private GeneratedResult generate(Type type, String schemaName) {
        return type == null ? null : generator.generate(type, schemaName);
    }

    private void setPath(SowaSchema sowaSchema, RestClass restClass, RestClassMethod method) {
        sowaSchema.setPathVariables(method.getPathVariables());
        sowaSchema.setFullEndpointPath(restClass.getEndpointPath() + method.getEndpointPath());
    }
}
