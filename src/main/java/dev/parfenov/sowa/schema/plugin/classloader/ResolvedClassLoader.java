package dev.parfenov.sowa.schema.plugin.classloader;

import com.fasterxml.classmate.*;

import java.lang.reflect.Type;

/**
 * Получение подробной информации о erased-классах
 */
public class ResolvedClassLoader {

    private final TypeResolver typeResolver = new TypeResolver();

    /**
     * Парсинг Class<?>
     *
     * @param type erased тип
     * @return расширенная информация erased-класса
     */
    public ResolvedType resolveErasedType(Type type) {
        return typeResolver.resolve(type);
    }

    /**
     * Парсит подробную информацию из ResolvedType
     *
     * @param type расширенная информация о erased-классе
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
