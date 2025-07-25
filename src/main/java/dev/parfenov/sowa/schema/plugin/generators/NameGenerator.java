/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.generators;

import dev.parfenov.sowa.schema.plugin.parsers.TypesParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;

import java.lang.reflect.Type;

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
     * @param classModel REST класс
     * @param method     REST метод
     * @return имя response-схемы:
     * <ul>
     *     <li>{@code null} при {@link MethodModel#getResponse()} == {@code null}</li>
     *     <li>{@code empty_object} для {@code void}</li>
     *     <li>в формате {@link #schemaID(ClassModel, MethodModel)} + {@link #RESPONSE_SUFFIX}</li>
     * </ul>
     */
    public static String responseSchemaName(ClassModel classModel, MethodModel method) {
        return schemaName(method.getResponse().getType(), classModel, method, RESPONSE_SUFFIX);
    }

    /**
     * Генерирует имя request-схемы
     *
     * @param classModel REST класс
     * @param method     REST метод
     * @return имя request-схемы:
     * <ul>
     *     <li>{@code null} при {@link MethodModel#getRequest()} == {@code null}</li>
     *     <li>в формате, {@link #schemaID(ClassModel, MethodModel)} + {@link #REQUEST_SUFFIX}</li>
     * </ul>
     */
    public static String requestSchemaName(ClassModel classModel, MethodModel method) {
        return schemaName(method.getRequest().getType(), classModel, method, REQUEST_SUFFIX);
    }

    private static String schemaName(Type type, ClassModel classModel, MethodModel method, String suffix) {
        return TypesParser.isVoid(type)
                ? EMPTY_OBJECT
                : schemaID(classModel, method).concat(suffix);
    }

    /**
     * Генерирует ID схемы на основе класса и метода.
     *
     * @param classModel  REST контроллер
     * @param methodModel метод контроллера
     * @return имя схемы в формате {@link ClassModel#getName()}{@code _}{@link MethodModel#getName()}
     */
    public static String schemaID(ClassModel classModel, MethodModel methodModel) {
        return classModel.getName().concat("_").concat(methodModel.getName());
    }
}
