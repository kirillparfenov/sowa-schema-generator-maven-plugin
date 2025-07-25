/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import com.fasterxml.classmate.*;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.TypeScope;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Парсер для работы с Java типами и их метаданными.
 * <p>
 * Использует библиотеку ClassMate для анализа типов и извлечения
 * информации о членах классов с учетом аннотаций.
 */
public class TypesParser {

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
     * <li>true, если {@code javaType == void}</li>
     * <li>false, если {@code javaType == null}, либо {@code javaType != void}</li>
     * </ul>
     */
    public static boolean isVoid(Type javaType) {
        return Optional
                .ofNullable(javaType)
                .map(type -> type.equals(void.class) || type.equals(Void.class))
                .orElse(false);
    }

    /**
     * Проверка, что {@code memberScore} является примитивом
     *
     * @param memberScope поле или метод
     * @return true, если {@code memberScore} является примитивом
     */
    public static boolean isPrimitive(MemberScope<?, ?> memberScope) {
        return Optional.ofNullable(memberScope)
                .map(TypeScope::getType)
                .map(ResolvedType::isPrimitive)
                .orElse(false);
    }
}
