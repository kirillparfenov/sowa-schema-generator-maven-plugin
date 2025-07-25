/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO для представления REST контроллера.
 * <p>
 * Содержит информацию о классе контроллера включая его имя,
 * базовый путь эндпоинта и список методов.
 */
public class ClassModel {
    /**
     * Имя класса контроллера
     */
    private String name;

    /**
     * Базовый путь эндпоинта из аннотации @RequestMapping на классе
     */
    private String endpointPath;

    /**
     * Список методов контроллера
     */
    private List<MethodModel> methods = new ArrayList<>();

    /*-------------------------------------------------------*/

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpointPath() {
        return endpointPath;
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public List<MethodModel> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodModel> methods) {
        this.methods = methods;
    }
}
