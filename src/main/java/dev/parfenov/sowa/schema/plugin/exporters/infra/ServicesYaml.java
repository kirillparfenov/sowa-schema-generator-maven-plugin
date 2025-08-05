/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporters.infra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "name", "url", "description", "allowed_queries", "chains", "!include"})
public class ServicesYaml {
    //указывает необходимость экспорта
    @JsonIgnore
    private boolean export = true;
    private String id;
    private String name;
    private String url;
    private String description;
    @JsonProperty("allowed_queries")
    private List<AllowedQuery> allowedQueries = new ArrayList<>();
    private Chains chains;
    @JsonProperty("!include")
    private String include = "common_for_http_service.yml";

    public boolean isExport() {
        return export;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<AllowedQuery> getAllowedQueries() {
        return allowedQueries;
    }

    public void setAllowedQueries(List<AllowedQuery> allowedQueries) {
        this.allowedQueries = allowedQueries;
    }

    public Chains getChains() {
        return chains;
    }

    public void setChains(Chains chains) {
        this.chains = chains;
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AllowedQuery {
        private String method;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"request_chains", "response_chains"})
    public static class Chains {
        @JsonProperty("request_chains")
        private List<Chain> requestChains;

        @JsonProperty("response_chains")
        private List<Chain> responseChains;

        public List<Chain> getRequestChains() {
            return requestChains;
        }

        public void setRequestChains(List<Chain> requestChains) {
            this.requestChains = requestChains;
        }

        public List<Chain> getResponseChains() {
            return responseChains;
        }

        public void setResponseChains(List<Chain> responseChains) {
            this.responseChains = responseChains;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"message", "actions"})
    public static class Chain {
        private String message;
        private List<Action> actions;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<Action> getActions() {
            return actions;
        }

        public void setActions(List<Action> actions) {
            this.actions = actions;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"action", "conditions", "params"})
    public static class Action {
        private String action;

        private List<Condition> conditions;

        private Param params;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public List<Condition> getConditions() {
            return conditions;
        }

        public void setConditions(List<Condition> conditions) {
            this.conditions = conditions;
        }

        public Param getParams() {
            return params;
        }

        public void setParams(Param params) {
            this.params = params;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"var", "operator", "val"})
    public static class Condition {
        private String var;
        private String operator;
        private String val;

        public String getVar() {
            return var;
        }

        public void setVar(String var) {
            this.var = var;
        }

        public String getVal() {
            return val;
        }

        public void setVal(String val) {
            this.val = val;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Param {
        @JsonProperty("validation_schema")
        private ValidationSchema validationSchema;

        @JsonProperty("max_allowable_size")
        private String maxAllowableSize;

        public ValidationSchema getValidationSchema() {
            return validationSchema;
        }

        public void setValidationSchema(ValidationSchema validationSchema) {
            this.validationSchema = validationSchema;
        }

        public String getMaxAllowableSize() {
            return maxAllowableSize;
        }

        public void setMaxAllowableSize(String maxAllowableSize) {
            this.maxAllowableSize = maxAllowableSize;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"type", "path"})
    public static class ValidationSchema {
        private String type;
        private String path;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
