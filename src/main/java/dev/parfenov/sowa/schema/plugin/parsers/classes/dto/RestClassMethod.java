package dev.parfenov.sowa.schema.plugin.parsers.classes.dto;

import org.springframework.http.HttpMethod;

import java.lang.reflect.Type;
import java.util.List;

public class RestClassMethod {
    /// Имя метода
    private String name;

    /// Путь над методом
    private String endpointPath;

    /// Тело запроса
    private Type request;

    /// Тело ответа
    private Type response;

    /// HTTP-метод
    private HttpMethod httpMethod;

    /// Переменные пути запроса
    private List<PathVariableInfo> pathVariables;

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

    public Type getRequest() {
        return request;
    }

    public void setRequest(Type request) {
        this.request = request;
    }

    public Type getResponse() {
        return response;
    }

    public void setResponse(Type response) {
        this.response = response;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<PathVariableInfo> getPathVariables() {
        return pathVariables;
    }

    public void setPathVariables(List<PathVariableInfo> pathVariables) {
        this.pathVariables = pathVariables;
    }


    @Override
    public String toString() {
        return "\nRestMethod{" +
                "\nmethodName='" + name + '\'' +
                ",\nendpointName='" + endpointPath + '\'' +
                ",\nrequest=" + request +
                ",\nresponse=" + response +
                ",\nhttpMethod=" + httpMethod +
                ",\npathVariableParams=" + pathVariables +
                "\n}";
    }
}
