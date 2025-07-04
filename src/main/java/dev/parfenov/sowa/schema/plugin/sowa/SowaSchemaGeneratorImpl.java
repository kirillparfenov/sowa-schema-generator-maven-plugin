package dev.parfenov.sowa.schema.plugin.sowa;

import dev.parfenov.sowa.schema.plugin.parsers.classes.EndpointPathResolver;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.generator.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.generator.Generator;
import org.apache.maven.project.MavenProject;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SowaSchemaGeneratorImpl implements SowaSchemaGenerator {

    private static final String REQUEST_SUFFIX = "_request";
    private static final String RESPONSE_SUFFIX = "_response";

    private final Generator generator;
    private final EndpointPathResolver pathResolver;

    public SowaSchemaGeneratorImpl(final Generator generator,
                                   final MavenProject mavenProject) {
        this.generator = generator;
        this.pathResolver = new EndpointPathResolver(mavenProject);
    }

    @Override
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
            sowaSchema.setRestClassName(restClass.getName());
            sowaSchema.setRestMethodName(method.getName());
            var endpointToSchema = pathResolver.endpointToSchema(restClass, method);
            var request = generate(method.getRequest(), endpointToSchema.concat(REQUEST_SUFFIX));
            var response = generate(method.getResponse(), endpointToSchema.concat(RESPONSE_SUFFIX));
            sowaSchema.setRequest(request);
            sowaSchema.setResponse(response);
            sowaSchema.setPathVariables(method.getPathVariables());
            sowaSchema.setFullEndpointPath(restClass.getEndpointPath() + method.getEndpointPath());
            schemas.add(sowaSchema);
        }
        return schemas;
    }

    private GeneratedResult generate(Type type, String schemaName) {
        if (type == null) {
            return null;
        }
        var mainSchema = generator.generate(type);
        var definitions = generator.extractDefinitions(mainSchema);
        generator.deleteDefinitions(mainSchema);
        return new GeneratedResult(schemaName, mainSchema, definitions);
    }
}
