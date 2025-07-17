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

public class Git {

    private static final String UNTRACKED_COMMAND = "git ls-files --other --exclude-standard";
    private static final String DIFF_COMMAND = "git diff --name-only %s";
    private final String gitDiffCommand;
    private final LazyValue<Set<String>> diffs = new LazyValue<>(this::searchDiff);

    public Git(String branchDiffWith) {
        this.gitDiffCommand = DIFF_COMMAND.formatted(branchDiffWith);
    }

    public Set<String> getDiff() {
        return diffs.get();
    }

    /**
     * Получить разницу между текущей веткой и develop
     *
     * @return список файлов с расширением .java, которые подверглись изменениями
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
            return diff.get(3, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Ошибка при выполнении команды GIT '" + gitDiffCommand + "'", e);
        }
    }

    private Set<String> extractDiff(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines()
                .map(this::extractSourceFile)
                .collect(Collectors.toSet());
    }

    private String extractSourceFile(String diff) {
        if (diff.contains(File.separator)) {
            return diff.substring(diff.lastIndexOf(File.separator) + 1);
        }
        return diff;
    }
}
