package dev.parfenov.sowa.schema.plugin.exporters.infra.factories;

import dev.parfenov.sowa.schema.plugin.exporters.infra.ServicesYaml;

import java.util.List;

/**
 * Фабрика для создания списка разрешенных запросов (allowed queries).
 *
 * <p>Отвечает за создание объектов {@link ServicesYaml.AllowedQuery}, которые
 * определяют разрешенные HTTP методы для обращения к сервису.</p>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-05
 */
public class AllowedQueryFactory {
    /**
     * Создает список разрешенных запросов для указанного HTTP метода.
     *
     * @param method HTTP метод (например, "get", "post", "put", "delete")
     * @return список с одним разрешенным запросом для указанного метода
     */
    public List<ServicesYaml.AllowedQuery> createAllowedQueries(String method) {
        var allowedQuery = new ServicesYaml.AllowedQuery();
        allowedQuery.setMethod(method);
        return List.of(allowedQuery);
    }
}
