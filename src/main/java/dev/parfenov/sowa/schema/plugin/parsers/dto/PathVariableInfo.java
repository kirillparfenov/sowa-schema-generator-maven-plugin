package dev.parfenov.sowa.schema.plugin.parsers.dto;

import java.lang.reflect.Type;

/**
 * DTO для представления переменной пути из аннотации @PathVariable.
 * <p>
 * Содержит имя переменной пути и её тип для дальнейшего
 * использования в генерации regex паттернов и валидации.
 */
public class PathVariableInfo {
    /**
     * Имя переменной пути
     */
    private String paramName;

    /**
     * Тип переменной пути
     */
    private Type paramType;

    /**
     * Создает информацию о переменной пути.
     *
     * @param paramName имя переменной пути
     * @param paramType тип переменной пути
     */
    public PathVariableInfo(String paramName, Class<?> paramType) {
        this.paramName = paramName;
        this.paramType = paramType;
    }

    /*-------------------------------------------------------*/

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public Type getParamType() {
        return paramType;
    }

    public void setParamType(Type paramType) {
        this.paramType = paramType;
    }

    @Override
    public String toString() {
        return "PathVariableInfo{" +
                "\nparamName='" + paramName + '\'' +
                ",\nparamType=" + paramType +
                "\n}";
    }
}
