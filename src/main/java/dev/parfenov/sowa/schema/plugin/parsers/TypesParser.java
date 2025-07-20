/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import com.fasterxml.classmate.*;

import java.lang.reflect.Type;
import java.util.Optional;

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

    /**
     * Проверяет является ли переданный тип {@link Void}
     *
     * @param javaType resolved - тип Java
     * @return true, если {@code javaType} == {@link Void}
     */
    public static boolean isVoid(ResolvedType javaType) {
        return isVoid(javaType.getErasedType());
    }

    /**
     * Проверяет является ли переданный тип {@link Void}
     *
     * @param javaType тип Java
     * @return <ul>
     *     <li>true, если {@code javaType == void}</li>
     *     <li>false, если {@code javaType == null}, либо {@code javaType != void}</li>
     * </ul>
     */
    public static boolean isVoid(Type javaType) {
        return Optional
                .ofNullable(javaType)
                .map(type -> type.equals(void.class) || type.equals(Void.class))
                .orElse(false);
    }
}
