/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;

import java.util.Optional;

/**
 * Генерирует имена схем
 */
public final class NameGenerator {

    private static final String REQUEST_SUFFIX = "_request";
    private static final String RESPONSE_SUFFIX = "_response";
    private static final String EMPTY_OBJECT = "empty_object";

    private NameGenerator() {
    }

    /**
     * Генерирует имя response-схемы
     *
     * @param restClass  REST класс
     * @param restMethod REST метод
     * @return имя response-схемы:
     * <ul>
     *     <li>{@code null} при {@link RestMethod#getResponse()} == {@code null}</li>
     *     <li>{@code empty_object} для {@code void}</li>
     *     <li>в формате {@link #schemaID(RestClass, RestMethod)} + {@link #RESPONSE_SUFFIX}</li>
     * </ul>
     */
    public static String responseSchemaName(RestClass restClass, RestMethod restMethod) {
        if (restMethod.getResponse() == null) return null;

        return TypesParser.isVoid(restMethod.getResponse())
                ? EMPTY_OBJECT
                : schemaID(restClass, restMethod).concat(RESPONSE_SUFFIX);
    }

    /**
     * Генерирует имя request-схемы
     *
     * @param restClass  REST класс
     * @param restMethod REST метод
     * @return имя request-схемы:
     * <ul>
     *     <li>{@code null} при {@link RestMethod#getRequest()} == {@code null}</li>
     *     <li>в формате, {@link #schemaID(RestClass, RestMethod)} + {@link #REQUEST_SUFFIX}</li>
     * </ul>
     */
    public static String requestSchemaName(RestClass restClass, RestMethod restMethod) {
        return Optional.ofNullable(restMethod.getRequest())
                .map(ignore -> schemaID(restClass, restMethod))
                .map(schemaId -> schemaId.concat(REQUEST_SUFFIX))
                .orElse(null);
    }

    /**
     * Генерирует ID схемы на основе класса и метода.
     *
     * @param restClass  REST контроллер
     * @param restMethod метод контроллера
     * @return имя схемы в формате {@link RestClass#getName()}{@code _}{@link RestMethod#getName()}
     */
    public static String schemaID(RestClass restClass, RestMethod restMethod) {
        return restClass.getName().concat("_").concat(restMethod.getName());
    }
}
