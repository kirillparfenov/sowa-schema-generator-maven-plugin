/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers.dto;

import dev.parfenov.sowa.schema.plugin.git.Dependencies;
import org.springframework.http.HttpMethod;

import java.lang.reflect.Type;
import java.util.List;

/**
 * DTO для представления метода REST контроллера.
 * <p>
 * Содержит всю необходимую информацию о методе включая типы запроса/ответа,
 * HTTP метод, путь эндпоинта и переменные пути.
 */
public class RestMethod {
    /**
     * Имя метода
     */
    private String name;

    /**
     * Путь эндпоинта из аннотации маппинга на методе
     */
    private String endpointPath;

    /**
     * Тип тела запроса (из @RequestBody)
     */
    private Type request;

    /**
     * Тип тела ответа (возвращаемый тип метода)
     */
    private Type response;

    /**
     * Зависимости source files для request/response
     */
    private Dependencies dependencies;

    /**
     * HTTP-метод (GET, POST, PUT, DELETE, etc.)
     */
    private HttpMethod httpMethod;

    /**
     * Переменные пути из аннотаций @PathVariable
     */
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

    public Dependencies getDependencies() {
        return dependencies;
    }

    public void setDependencies(Dependencies dependencies) {
        this.dependencies = dependencies;
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
}
