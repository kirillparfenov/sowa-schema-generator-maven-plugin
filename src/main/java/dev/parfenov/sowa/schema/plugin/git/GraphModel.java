package dev.parfenov.sowa.schema.plugin.git;

import io.github.classgraph.ClassInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GraphModel {
    private final ClassInfo current;
    private final List<GraphModel> dependencies;
    private final int genericsHash;

    public GraphModel(ClassInfo current, int genericsHash) {
        this.current = current;
        this.dependencies = new ArrayList<>();
        this.genericsHash = genericsHash;
    }

    public void append(GraphModel dependency) {
        this.dependencies.add(dependency);
    }

    public ClassInfo getCurrent() {
        return current;
    }

    public List<GraphModel> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        GraphModel that = (GraphModel) object;
        return Objects.equals(current, that.current) && Objects.equals(genericsHash, that.genericsHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(current, genericsHash);
    }
}
