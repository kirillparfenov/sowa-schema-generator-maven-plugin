package dev.parfenov.sowa.schema.plugin.git;

import com.github.victools.jsonschema.generator.impl.LazyValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Утилита для работы с git командами.
 * <p>
 * Предоставляет методы для получения списка измененных файлов
 * между текущей веткой и указанной базовой веткой.
 */
public class Git {

    private static final String UNTRACKED_COMMAND = "git ls-files --other --exclude-standard";
    private static final String DIFF_COMMAND = "git diff --name-only %s";
    
    /** Таймаут выполнения git команд в секундах */
    private static final int GIT_COMMAND_TIMEOUT_SECONDS = 3;
    
    private final String gitDiffCommand;
    private final LazyValue<Set<String>> diffs = new LazyValue<>(this::searchDiff);

    /**
     * Создает экземпляр для работы с git diff.
     * 
     * @param branchDiffWith ветка для сравнения (например, "origin/develop")
     */
    public Git(String branchDiffWith) {
        this.gitDiffCommand = DIFF_COMMAND.formatted(branchDiffWith);
    }

    /**
     * Возвращает множество имен файлов, измененных относительно базовой ветки.
     * <p>
     * Результат кешируется при первом обращении.
     * 
     * @return множество имен измененных файлов
     */
    public Set<String> getDiff() {
        return diffs.get();
    }

    /**
     * Выполняет git diff команду и возвращает список измененных файлов.
     * 
     * @return множество имен файлов, которые подверглись изменениям
     * @throws RuntimeException если команда git завершилась с ошибкой или таймаутом
     */
    private Set<String> searchDiff() {
        try {
            var process = Runtime.getRuntime().exec(gitDiffCommand);
            var diff = Executors.newSingleThreadExecutor().submit(() -> extractDiff(process));
            var exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new RuntimeException(
                        "Выполнение команды GIT '" + gitDiffCommand + "' завершилось неудачно с кодом: " + exitCode
                );
            }
            
            return diff.get(GIT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Ошибка при выполнении команды GIT '" + gitDiffCommand + "'", e);
        }
    }

    /**
     * Извлекает список файлов из вывода git процесса.
     * 
     * @param process процесс выполнения git команды
     * @return множество имен файлов
     */
    private Set<String> extractDiff(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines()
                .map(this::extractSourceFile)
                .collect(Collectors.toSet());
    }

    /**
     * Извлекает имя файла из полного пути.
     * <p>
     * Преобразует путь вида "src/main/java/com/example/Class.java" в "Class.java".
     * 
     * @param diff полный путь к файлу
     * @return имя файла без пути
     */
    private String extractSourceFile(String diff) {
        if (diff.contains(File.separator)) {
            return diff.substring(diff.lastIndexOf(File.separator) + 1);
        }
        return diff;
    }
}
