/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.git;

import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * Парсер git diff для определения необходимости генерации схем.
 * <p>
 * Анализирует изменения в git репозитории и определяет, для каких методов
 * REST контроллеров необходимо генерировать схемы запросов и ответов.
 * <p>
 * Основная логика:
 * <ul>
 *   <li>Получает список измененных файлов из git diff</li>
 *   <li>Строит граф зависимостей классов</li>
 *   <li>Для каждого метода проверяет, изменились ли связанные классы</li>
 *   <li>Обнуляет схемы для неизмененных типов</li>
 * </ul>
 */
public class GitDiffParser {

    private final Git git;

    public GitDiffParser(final String branchDiffWith) {
        this.git = new Git(branchDiffWith);
    }

    /**
     * Анализирует изменения и обнуляет схемы для неизмененных типов.
     * <p>
     * Для каждого метода REST класса проверяет, изменились ли типы запроса или ответа.
     * Если тип не изменился, устанавливает его в null, чтобы избежать генерации схемы.
     *
     * @param parsedClasses список проанализированных REST классов
     */
    public void diffMethods(List<RestClass> parsedClasses) {
        if (CollectionUtils.isEmpty(parsedClasses)) {
            return;
        }

        for (var restClass : parsedClasses) {
            processRestClassMethods(restClass);
        }
    }

    /**
     * Обрабатывает методы REST класса и определяет необходимость генерации схем.
     */
    private void processRestClassMethods(RestClass restClass) {
        if (CollectionUtils.isEmpty(restClass.getMethods())) {
            return;
        }

        for (var method : restClass.getMethods()) {
            var responseChanged = isTypeChanged(method.getDependencies().getResponse());
            var requestChanged = isTypeChanged(method.getDependencies().getRequest());

            updateMethodTypes(method, responseChanged, requestChanged);
        }
    }

    /**
     * Проверяет, изменился ли указанный тип.
     *
     * @param sourceFiles source-файлы зависимостей классов
     * @return true если тип изменился
     */
    private boolean isTypeChanged(Set<String> sourceFiles) {
        if (sourceFiles.isEmpty()) {
            return false;
        }
        return CollectionUtils.containsAny(git.getDiff(), sourceFiles);
    }

    /**
     * Обновляет типы метода на основе результатов анализа изменений.
     */
    private void updateMethodTypes(RestMethod method, boolean responseChanged, boolean requestChanged) {
        if (!responseChanged) {
            method.setResponse(null);
        }
        if (!requestChanged) {
            method.setRequest(null);
        }
    }
}
