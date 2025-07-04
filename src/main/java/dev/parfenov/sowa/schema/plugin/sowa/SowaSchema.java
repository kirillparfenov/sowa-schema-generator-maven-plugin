package dev.parfenov.sowa.schema.plugin.sowa;

import dev.parfenov.sowa.schema.plugin.generator.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.PathVariableInfo;
import org.springframework.http.HttpMethod;

import java.util.List;

public class SowaSchema {
    private GeneratedResult request;
    private GeneratedResult response;
    private String restClassName;
    private String restMethodName;
    private HttpMethod httpMethod;
    private String fullEndpointPath;
    private List<PathVariableInfo> pathVariables;

    /*-------------------------------------------------------*/

    public GeneratedResult getRequest() {
        return request;
    }

    public void setRequest(GeneratedResult request) {
        this.request = request;
    }

    public GeneratedResult getResponse() {
        return response;
    }

    public void setResponse(GeneratedResult response) {
        this.response = response;
    }

    public String getRestClassName() {
        return restClassName;
    }

    public void setRestClassName(String restClassName) {
        this.restClassName = restClassName;
    }

    public String getRestMethodName() {
        return restMethodName;
    }

    public void setRestMethodName(String restMethodName) {
        this.restMethodName = restMethodName;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getFullEndpointPath() {
        return fullEndpointPath;
    }

    public void setFullEndpointPath(String fullEndpointPath) {
        this.fullEndpointPath = fullEndpointPath;
    }

    public List<PathVariableInfo> getPathVariables() {
        return pathVariables;
    }

    public void setPathVariables(List<PathVariableInfo> pathVariables) {
        this.pathVariables = pathVariables;
    }
}
