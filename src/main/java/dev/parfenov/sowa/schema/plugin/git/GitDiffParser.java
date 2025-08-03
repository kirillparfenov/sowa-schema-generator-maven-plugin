package dev.parfenov.sowa.schema.plugin.git;

import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.Entity;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;
import org.springframework.util.CollectionUtils;

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
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class GitDiffParser {

    private final Git git;
    private boolean onlyGitDiff;

    public GitDiffParser(final String branchDiffWith, final boolean onlyGitDiff) {
        this.git = new Git(branchDiffWith);
        this.onlyGitDiff = onlyGitDiff;
    }

    /**
     * Для каждого метода REST класса проверяет, изменились ли типы запроса или ответа.
     * Если тип изменился - устанавливает {@link Entity#setCanExport(boolean)} в {@code false}, чтобы избежать генерации схемы.
     *
     * @param restClass найденный REST класс
     */
    public void diff(ClassModel restClass) {
        if (onlyGitDiff) {
            processRestClassMethods(restClass);
        }
    }

    /**
     * Обрабатывает методы REST класса и определяет необходимость генерации схем.
     */
    private void processRestClassMethods(ClassModel classModel) {
        if (CollectionUtils.isEmpty(classModel.getMethods())) {
            return;
        }

        for (var method : classModel.getMethods()) {
            var responseChanged = hasDiff(method.getResponse().getDependencies());
            var requestChanged = hasDiff(method.getRequest().getDependencies());

            updateMethodTypes(method, responseChanged, requestChanged);
        }
    }

    /**
     * Проверяет, изменился ли указанный тип.
     *
     * @param sourceFiles source-файлы зависимостей классов
     * @return true если тип изменился
     */
    private boolean hasDiff(Set<String> sourceFiles) {
        return CollectionUtils.containsAny(git.getDiff(), sourceFiles);
    }

    /**
     * Обновляет типы метода на основе результатов анализа изменений.
     */
    private void updateMethodTypes(MethodModel method, boolean responseChanged, boolean requestChanged) {
        if (!responseChanged) {
            method.getResponse().setCanExport(false);
        }
        if (!requestChanged) {
            method.getRequest().setCanExport(false);
        }
    }
}
