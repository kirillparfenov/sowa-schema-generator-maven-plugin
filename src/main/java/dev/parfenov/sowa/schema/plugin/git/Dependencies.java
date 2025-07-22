package dev.parfenov.sowa.schema.plugin.git;


import java.util.HashSet;
import java.util.Set;

public class Dependencies {
    private final Set<String> response = new HashSet<>();
    private final Set<String> request = new HashSet<>();

    public Set<String> getResponse() {
        return response;
    }

    public Set<String> getRequest() {
        return request;
    }
}
