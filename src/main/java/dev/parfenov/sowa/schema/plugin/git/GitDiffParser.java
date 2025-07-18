package dev.parfenov.sowa.schema.plugin.git;

import com.fasterxml.classmate.ResolvedType;
import dev.parfenov.sowa.schema.plugin.parsers.TypesParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

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
    private final TypesParser typesParser = new TypesParser();
    private final ScanResult scanResult;

    /**
     * Создает парсер git diff.
     *
     * @param branchDiffWith ветка для сравнения (например, "origin/develop")
     * @param scanResult     результат построения графа классов
     */
    public GitDiffParser(final String branchDiffWith, final ScanResult scanResult) {
        this.git = new Git(branchDiffWith);
        this.scanResult = scanResult;
    }

    /**
     * Анализирует изменения и обнуляет схемы для неизмененных типов.
     * <p>
     * Для каждого метода REST класса проверяет, изменились ли типы запроса или ответа.
     * Если тип не изменился, устанавливает его в null, чтобы избежать генерации схемы.
     *
     * @param parsedClasses список проанализированных REST классов
     */
    public void setNullForNoDiff(List<RestClass> parsedClasses) {
        if (CollectionUtils.isEmpty(parsedClasses)) {
            return;
        }

        var sourceDependencies = buildDependencyMap();

        for (var restClass : parsedClasses) {
            processRestClassMethods(restClass, sourceDependencies);
        }
    }

    /**
     * Строит карту зависимостей между исходными файлами.
     *
     * @return карта где ключ - файл, значение - множество зависимых файлов
     */
    private Map<String, Set<String>> buildDependencyMap() {
        return scanResult.getClassDependencyMap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getSourceFile(),
                        entry -> entry.getValue().stream()
                                .map(ClassInfo::getSourceFile)
                                .collect(Collectors.toSet()),
                        this::mergeDependencySets
                ));
    }

    /**
     * Объединяет множества зависимостей при коллизии ключей.
     */
    private Set<String> mergeDependencySets(Set<String> before, Set<String> current) {
        var merged = new HashSet<>(before);
        merged.addAll(current);
        return merged;
    }

    /**
     * Обрабатывает методы REST класса и определяет необходимость генерации схем.
     */
    private void processRestClassMethods(RestClass restClass,
                                         Map<String, Set<String>> sourceDependencies) {
        if (CollectionUtils.isEmpty(restClass.getMethods())) {
            return;
        }

        for (var method : restClass.getMethods()) {
            var responseChanged = isTypeChanged(method.getResponse(), sourceDependencies);
            var requestChanged = isTypeChanged(method.getRequest(), sourceDependencies);

            updateMethodTypes(method, responseChanged, requestChanged);
        }
    }

    /**
     * Проверяет, изменился ли указанный тип.
     *
     * @param type               тип для проверки
     * @param sourceDependencies карта зависимостей
     * @return true если тип изменился
     */
    private boolean isTypeChanged(Type type,
                                  Map<String, Set<String>> sourceDependencies) {
        var rootClasses = extractGenericClasses(type);
        return hasChangesInClasses(rootClasses, sourceDependencies);
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

    /**
     * Извлекает классы из generic-типа.
     * <p>
     * Для User.class вернет User
     * <p>
     * Для {@literal Map<UUID, List<User>>} вернет Map, UUID, List, User
     *
     * @param root корневой тип
     * @return множество классов, составляющих тип
     */
    private Set<Class<?>> extractGenericClasses(Type root) {
        if (root == null) {
            return null;
        }

        var resolvedRoot = typesParser.resolveErasedType(root);
        var rootClasses = new HashSet<Class<?>>();
        extractClassesRecursively(resolvedRoot, rootClasses);
        return rootClasses;
    }

    /**
     * Рекурсивно извлекает все классы из типа включая параметры обобщений.
     *
     * @param type        разрешенный тип
     * @param rootClasses множество для накопления классов
     */
    private void extractClassesRecursively(ResolvedType type, Set<Class<?>> rootClasses) {
        rootClasses.add(type.getErasedType());
        for (ResolvedType param : type.getTypeParameters()) {
            extractClassesRecursively(param, rootClasses);
        }
    }

    /**
     * Проверяет наличие изменений в указанных классах.
     *
     * @param rootClasses        множество классов для проверки
     * @param sourceDependencies карта зависимостей исходных файлов
     * @return true если найдены изменения в классах или их зависимостях
     */
    private boolean hasChangesInClasses(Set<Class<?>> rootClasses,
                                        Map<String, Set<String>> sourceDependencies) {
        if (CollectionUtils.isEmpty(rootClasses)) {
            return false;
        }

        for (var rootClass : rootClasses) {
            var sourceFile = getSourceFileForClass(rootClass);
            if (sourceFile.isBlank()) {
                continue;
            }

            if (hasChangesInFile(sourceFile, sourceDependencies)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Получает имя исходного файла для класса.
     *
     * @param clazz класс
     * @return имя файла или пустая строка
     */
    private String getSourceFileForClass(Class<?> clazz) {
        return Optional
                .ofNullable(scanResult.getClassInfo(clazz.getName()))
                .map(ClassInfo::getSourceFile)
                .orElse("");
    }

    /**
     * Проверяет наличие изменений в файле и его зависимостях.
     *
     * @param sourceFile         проверяемый файл
     * @param sourceDependencies карта зависимостей
     * @return true если найдены изменения
     */
    private boolean hasChangesInFile(String sourceFile,
                                     Map<String, Set<String>> sourceDependencies) {
        return hasChangesInFileRecursively(git.getDiff(), sourceFile, sourceDependencies, new HashSet<>());
    }

    /**
     * Рекурсивно ищет изменения в файле и его зависимостях с защитой от циклов.
     *
     * @param gitDiff            множество измененных файлов
     * @param sourceFile         текущий проверяемый файл
     * @param sourceDependencies карта зависимостей файлов
     * @param visitedFiles       множество уже посещенных файлов (защита от циклов)
     * @return true если найдены изменения в файле или его зависимостях
     */
    private boolean hasChangesInFileRecursively(Set<String> gitDiff,
                                                String sourceFile,
                                                Map<String, Set<String>> sourceDependencies,
                                                Set<String> visitedFiles) {
        // Защита от циклических зависимостей
        if (visitedFiles.contains(sourceFile)) {
            return false;
        }
        visitedFiles.add(sourceFile);

        // Проверяем наличие diff
        if (CollectionUtils.isEmpty(gitDiff)) {
            return false;
        }

        // Прямая проверка изменений в файле
        if (gitDiff.contains(sourceFile)) {
            return true;
        }

        // Проверяем наличие зависимостей
        if (!sourceDependencies.containsKey(sourceFile)) {
            return false;
        }

        // Рекурсивно проверяем зависимости
        for (var dependentFile : sourceDependencies.get(sourceFile)) {
            if (hasChangesInFileRecursively(gitDiff, dependentFile, sourceDependencies, visitedFiles)) {
                return true;
            }
        }

        return false;
    }
}
