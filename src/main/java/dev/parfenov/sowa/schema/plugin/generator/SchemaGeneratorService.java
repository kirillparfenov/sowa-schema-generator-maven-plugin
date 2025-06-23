package dev.parfenov.sowa.schema.plugin.generator;

import com.fasterxml.classmate.*;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import dev.parfenov.sowa.schema.plugin.config.CustomJakartaValidationModule;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

public class SchemaGeneratorService {

    private final ObjectMapper mapper;
    private final SchemaGenerator generator;
    private final TypeResolver typeResolver;

    public SchemaGeneratorService() {
        typeResolver = new TypeResolver();
        mapper = new ObjectMapper();
        generator = new SchemaGenerator(
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                        .with(new JacksonModule())
                        .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                        .with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                        .with(Option.NULLABLE_FIELDS_BY_DEFAULT)
                        .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                        .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                        .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                        .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                        /// если не передать JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS - не будет учитываться @Pattern
                        /// и даже не передастся String resolveStringPattern(MemberScope<?, ?> member) в  SchemaGeneratorConfigPart<?>.withStringPatternResolver
                        .with(new CustomJakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS))
                        .build()
        );
    }

    /**
     * Генерация схемы из расширенного erased-типа
     *
     * @param type расширенный erased-тип
     * @return JSON-схема
     */
    public ObjectNode generateSchema(ResolvedType type) {
        return generator.generateSchema(type);
    }

    /**
     * Парсинг Class<?>
     *
     * @param type erased тип
     * @return класс-реализация Type с указанием generics
     */
    public ResolvedType resolveErasedType(Type type) {
        return typeResolver.resolve(type);
    }

    /**
     * Парсит подробную информацию о классе из ResolvedType
     *
     * @param type класс-реализация Type
     * @return подробную информацию о классе
     */
    public ResolvedTypeWithMembers resolveTypeMembers(ResolvedType type) {
        return new MemberResolver(typeResolver).resolve(
                type,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED),
                null
        );
    }

    /**
     * Генерация JSON-схем из возвращаемых типов методов
     *
     * @param methods подробная информация о методах
     * @return массив JSON-схем
     */
    public List<ObjectNode> generateEachReturnType(ResolvedMethod[] methods) {
        return Stream.of(methods)
                .map(ResolvedMethod::getReturnType)
                .map(this::generateSchema)
                .toList();
    }
}
