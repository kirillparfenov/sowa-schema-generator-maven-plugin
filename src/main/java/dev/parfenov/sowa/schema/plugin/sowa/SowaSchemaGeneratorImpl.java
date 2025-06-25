package dev.parfenov.sowa.schema.plugin.sowa;

import com.fasterxml.classmate.ResolvedType;
import dev.parfenov.sowa.schema.plugin.classparser.ClassMethod;
import dev.parfenov.sowa.schema.plugin.generator.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.generator.Generator;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class SowaSchemaGeneratorImpl implements SowaSchemaGenerator {

    private static final String REQUEST_SUFFIX = "_request";
    private static final String RESPONSE_SUFFIX = "_response";
    private final Generator generator;

    public SowaSchemaGeneratorImpl(Generator generator) {
        this.generator = generator;
    }

    @Override
    public List<SowaSchema> generateSchema(List<ClassMethod> restMethods) {
        if (CollectionUtils.isEmpty(restMethods)) {
            return List.of();
        }
        try {
            return restMethods
                    .stream()
                    .map(this::generate)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время генерации схемы из " + restMethods, e);
        }
    }

    private SowaSchema generate(ClassMethod restMethod) {
        var restName = restMethod.restControllerMethodName();
        var request = generate(restMethod.request(), restName.concat(REQUEST_SUFFIX));
        var response = generate(restMethod.response(), restName.concat(RESPONSE_SUFFIX));
        return new SowaSchema(request, response, restName, restMethod.httpMethod());
    }

    private GeneratedResult generate(ResolvedType type, String schemaName) {
        if (type == null) {
            return null;
        }
        var mainSchema = generator.generate(type);
        var definitions = generator.extractDefinitions(mainSchema);
        generator.deleteDefinitions(mainSchema);
        return new GeneratedResult(schemaName, mainSchema, definitions);
    }
}
