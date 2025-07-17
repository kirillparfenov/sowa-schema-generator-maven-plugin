package dev.parfenov.sowa.schema.plugin.parsers;

import com.fasterxml.classmate.*;

import java.lang.reflect.Type;

public class TypesParser {

    private final TypeResolver typeResolver = new TypeResolver();

    /**
     * Парсинг Type
     *
     * @param type тип
     * @return информация о типе
     */
    public ResolvedType resolveErasedType(Type type) {
        return typeResolver.resolve(type);
    }

    /**
     * Парсит подробную информацию из ResolvedType
     *
     * @param type информация о типе
     * @return подробную информацию о классе
     */
    public ResolvedTypeWithMembers resolveTypeMembers(ResolvedType type) {
        var memberResolver = new MemberResolver(typeResolver);
        return memberResolver.resolve(
                type,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED),
                null
        );
    }
}
