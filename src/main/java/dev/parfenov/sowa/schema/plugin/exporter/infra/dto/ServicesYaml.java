package dev.parfenov.sowa.schema.plugin.exporter.infra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "name", "url", "description", "allowed_queries", "validators", "!include"})
public class ServicesYaml {
    @JsonProperty("id")
    private String id;
    private String name;
    private String url;
    private String description;
    @JsonProperty("allowed_queries")
    private List<AllowedQuery> allowedQueries = new ArrayList<>();
    private Validator validators;
    @JsonProperty("!include")
    private String include = "common_for_http_service.yml";

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

    public Validator getValidators() {
        return validators;
    }

    public void setValidators(Validator validators) {
        this.validators = validators;
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
    public static class Validator {
        @JsonProperty("validator_json")
        private ValidatorJson validatorJson;

        public ValidatorJson getValidatorJson() {
            return validatorJson;
        }

        public void setValidatorJson(ValidatorJson validatorJson) {
            this.validatorJson = validatorJson;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidatorJson {
        private List<RequestResponse> response;
        private List<RequestResponse> request;

        public List<RequestResponse> getResponse() {
            return response;
        }

        public void setResponse(List<RequestResponse> response) {
            this.response = response;
        }

        public List<RequestResponse> getRequest() {
            return request;
        }

        public void setRequest(List<RequestResponse> request) {
            this.request = request;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestResponse {
        private String method;
        private String schema;
        @JsonProperty("allow_empty_body")
        private boolean allowEmptyBody;
        @JsonProperty("response_code")
        private ResponseCode responseCode;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public boolean isAllowEmptyBody() {
            return allowEmptyBody;
        }

        public void setAllowEmptyBody(boolean allowEmptyBody) {
            this.allowEmptyBody = allowEmptyBody;
        }

        public ResponseCode getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(ResponseCode responseCode) {
            this.responseCode = responseCode;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseCode {
        private char operator;
        private String pattern;

        public char getOperator() {
            return operator;
        }

        public void setOperator(char operator) {
            this.operator = operator;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
    }
}
