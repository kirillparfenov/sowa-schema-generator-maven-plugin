package dev.parfenov.sowa.schema.plugin.parsers.dto;

import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * DTO для представления метода REST контроллера.
 * <p>
 * Содержит всю необходимую информацию о методе включая типы запроса/ответа,
 * HTTP метод, путь эндпоинта и переменные пути.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class MethodModel {
    /**
     * Имя метода
     */
    private String name;

    /**
     * Путь эндпоинта из аннотации маппинга на методе
     */
    private String endpointPath;

    /**
     * Сущность тела запроса - помечается @{@link RequestBody}
     */
    private Entity request = new Entity();

    /**
     * Сущность тела ответа (возвращаемый тип метода)
     */
    private Entity response = new Entity();

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

    public Entity getRequest() {
        return request;
    }

    public void setRequest(Entity request) {
        this.request = request;
    }

    public Entity getResponse() {
        return response;
    }

    public void setResponse(Entity response) {
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
}
