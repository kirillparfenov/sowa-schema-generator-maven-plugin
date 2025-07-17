package dev.parfenov.sowa.schema.plugin.parsers;

import com.fasterxml.classmate.*;

import java.lang.reflect.Type;

/**
 * Парсер для работы с Java типами и их метаданными.
 * <p>
 * Использует библиотеку ClassMate для анализа типов и извлечения
 * информации о членах классов с учетом аннотаций.
 */
public class TypesParser {

    private final TypeResolver typeResolver = new TypeResolver();

    /**
     * Разрешает информацию о типе.
     *
     * @param type Java тип для анализа
     * @return информация о разрешенном типе
     */
    public ResolvedType resolveErasedType(Type type) {
        return typeResolver.resolve(type);
    }

    /**
     * Извлекает подробную информацию о членах типа.
     *
     * @param type разрешенный тип для анализа
     * @return подробная информация о классе включая методы, поля и аннотации
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
