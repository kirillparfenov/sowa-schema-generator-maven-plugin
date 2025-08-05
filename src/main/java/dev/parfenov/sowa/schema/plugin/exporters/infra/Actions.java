package dev.parfenov.sowa.schema.plugin.exporters.infra;

public enum Actions {
    JSON_VALIDATION("JsonValidation"),
    CHECK_DATA_SIZE("CheckDataSize");

    private final String action;

    Actions(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
