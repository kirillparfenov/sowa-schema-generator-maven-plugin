/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.sowa;

import dev.parfenov.sowa.schema.plugin.generator.dto.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.parsers.dto.PathVariableInfo;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * Модель данных для схемы Sowa.
 * <p>
 * Содержит всю информацию о REST эндпоинте включая схемы запроса/ответа,
 * метаданные контроллера и метода, HTTP метод и путь.
 */
public class SowaSchema {
    /**
     * Схема запроса эндпоинта
     */
    private GeneratedResult request;

    /**
     * Схема ответа эндпоинта
     */
    private GeneratedResult response;

    /**
     * Имя REST контроллера
     */
    private String restClassName;

    /**
     * Имя метода контроллера
     */
    private String restMethodName;

    /**
     * HTTP метод эндпоинта
     */
    private HttpMethod httpMethod;

    /**
     * Полный путь эндпоинта
     */
    private String fullEndpointPath;

    /**
     * Список переменных пути
     */
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
