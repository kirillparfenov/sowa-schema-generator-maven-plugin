package dev.parfenov.sowa.schema.plugin.generator;

import com.github.victools.jsonschema.generator.MemberScope;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Утилиты для извлечения значений из аннотаций Schema и ArraySchema.
 * <p>
 * Предоставляет единообразные методы для получения значений из различных
 * аннотаций валидации с учетом приоритетов и фильтрации.
 */
public final class MemberAnnotationExtractor {

    private MemberAnnotationExtractor() {
    }

    /**
     * Извлекает значение из аннотации Schema с учетом ArraySchema.
     *
     * @param <T>            тип извлекаемого значения
     * @param member         область видимости члена класса
     * @param valueExtractor функция извлечения значения из Schema
     * @param valueFilter    предикат для фильтрации значений
     * @return извлеченное значение или empty Optional
     */
    public static <T> Optional<T> getSchemaAnnotationValue(MemberScope<?, ?> member,
                                                           Function<Schema, T> valueExtractor,
                                                           Predicate<T> valueFilter) {
        if (member.isFakeContainerItemScope()) {
            return getArraySchemaAnnotation(member)
                    .map(ArraySchema::schema)
                    .map(valueExtractor)
                    .filter(valueFilter);
        }

        var annotation = member.getAnnotationConsideringFieldAndGetter(Schema.class);
        if (annotation != null) {
            return Optional.of(annotation)
                    .map(valueExtractor)
                    .filter(valueFilter);
        }

        return getArraySchemaAnnotation(member)
                .map(ArraySchema::arraySchema)
                .map(valueExtractor)
                .filter(valueFilter);
    }

    /**
     * Получает аннотацию ArraySchema из области видимости.
     *
     * @param member область видимости члена класса
     * @return Optional с аннотацией ArraySchema
     */
    public static Optional<ArraySchema> getArraySchemaAnnotation(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetter(ArraySchema.class));
    }
} 