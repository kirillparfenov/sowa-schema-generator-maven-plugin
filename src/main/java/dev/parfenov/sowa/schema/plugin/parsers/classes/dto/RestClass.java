package dev.parfenov.sowa.schema.plugin.parsers.classes.dto;

import java.util.List;

public class RestClass {
    private String name;
    private String endpointPath;
    private List<RestClassMethod> methods;

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

    public List<RestClassMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<RestClassMethod> methods) {
        this.methods = methods;
    }

    @Override
    public String toString() {
        return "RestClass{" +
                "\nendpointPath='" + endpointPath + '\'' +
                ",\nmethods=" + methods +
                '}';
    }
}
