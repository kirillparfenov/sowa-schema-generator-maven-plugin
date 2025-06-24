package dev.parfenov.sowa.schema.plugin.generator;

import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import dev.parfenov.sowa.schema.plugin.config.CustomJakartaValidationModule;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class SchemaGeneratorService {

    private final ObjectMapper mapper;
    private final SchemaGenerator generator;

    public SchemaGeneratorService() {
        mapper = new ObjectMapper();
        var configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                .with(new JacksonModule())
                .with(new Swagger2Module())
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                .with(Option.NULLABLE_FIELDS_BY_DEFAULT)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                /// если не передать JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS - не будет учитываться @Pattern
                /// и даже не передастся String resolveStringPattern(MemberScope<?, ?> member) в  SchemaGeneratorConfigPart<?>.withStringPatternResolver
                .with(new CustomJakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS));

        configBuilder.forTypesInGeneral()
                .withStringMinLengthResolver(scope -> scope.getType().getErasedType() == UUID.class ? 36 : null);

        generator = new SchemaGenerator(configBuilder.build());
    }

    /**
     * Генерация JSON-схем из возвращаемых типов методов
     *
     * @param methods подробная информация о методах
     * @return массив JSON-схем
     */
//    public List<RestControllerMethod> generateEachReturnType(ResolvedMethod[] methods) {
//        return Stream.of(methods)
//                .map(ResolvedMethod::getReturnType)
//                .map(this::generateSchema)
//                .toList();
//    }

    /**
     * Генерация схемы из расширенного erased-типа
     *
     * @param type расширенный erased-тип
     * @return JSON-схема
     */
//    public RestControllerMethod generateSchema(Type type) {
//        System.out.println("typeName: " + type.getTypeName());
//        var schema = generator.generateSchema(type);
//        return new RestControllerMethod(schema, type.getTypeName());
//    }

    /**
     * Бьютификация схемы и печать в консоль
     */
//    public void prettyPrint(RestControllerMethod restcontrollerMethod) {
//        var definitions = restcontrollerMethod.schema().get("definitions");
//        System.out.println("definitions print: ");
//        prettyPrintSchema(definitions);
//        removeDefinitions(restcontrollerMethod);
//
//        System.out.println("schema print without definitions: ");
//        prettyPrintSchema(restcontrollerMethod.schema());
//        System.out.println("\n");
//    }

    private void prettyPrintSchema(JsonNode schema) {
        try {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка во время вывода pretty json", e);
        }
    }

//    private void removeDefinitions(RestControllerMethod restcontrollerMethod) {
//        restcontrollerMethod.schema().remove("definitions");
//    }
}
