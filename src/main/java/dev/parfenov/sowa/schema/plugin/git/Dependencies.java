package dev.parfenov.sowa.schema.plugin.git;

import java.util.HashSet;
import java.util.Set;

/**
 * Класс для хранения зависимостей запросов и ответов.
 * <p>
 * Используется для отслеживания связей между типами в git diff анализе.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
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
