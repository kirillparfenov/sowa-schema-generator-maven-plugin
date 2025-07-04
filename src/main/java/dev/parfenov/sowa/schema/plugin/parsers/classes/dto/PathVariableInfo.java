package dev.parfenov.sowa.schema.plugin.parsers.classes.dto;

import java.lang.reflect.Type;

public class PathVariableInfo {
    private String paramName;
    private Type paramType;

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
        return "\nPathVariableParam{" +
                "\nparamName='" + paramName + '\'' +
                ",\nparamType=" + paramType +
                "\n}";
    }
}
