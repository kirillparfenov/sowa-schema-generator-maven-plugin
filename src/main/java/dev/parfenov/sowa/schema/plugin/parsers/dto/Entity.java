package dev.parfenov.sowa.schema.plugin.parsers.dto;

import dev.parfenov.sowa.schema.plugin.generators.dto.GeneratedResult;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Request/Response сущность метода
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class Entity {

    /**
     * Тип сущности
     */
    private Type type;

    /**
     * Сгенерированная схема
     */
    private GeneratedResult schema;

    /**
     * Зависимости сущности от других классов в виде source-файлов
     */
    private Set<String> dependencies;

    /**
     * Пометка о необходимости экспортировать схему.
     * По-умолчанию {@code true}
     */
    private boolean canExport = true;

    //----------------------------------//

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public GeneratedResult getSchema() {
        return schema;
    }

    public void setSchema(GeneratedResult schema) {
        this.schema = schema;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    public boolean canExport() {
        return canExport;
    }

    public void setCanExport(boolean canExport) {
        this.canExport = canExport;
    }
}
