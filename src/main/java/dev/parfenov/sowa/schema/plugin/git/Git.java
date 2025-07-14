package dev.parfenov.sowa.schema.plugin.git;

import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
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
    private final MavenProject project;

    public Git(String branchDiffWith, MavenProject project) {
        this.project = project;
        this.gitDiffCommand =
                DIFF_COMMAND.formatted(branchDiffWith);
//                        .concat(" && ")
//                        .concat(UNTRACKED_COMMAND);
    }

    /**
     * Получить разницу между текущей веткой и develop
     *
     * @return список файлов с расширением .java, которые подверглись изменениями
     */
    public Set<String> getDiff() {
        try {
            var process = Runtime.getRuntime().exec(gitDiffCommand);
            var diff = Executors.newSingleThreadExecutor().submit(() -> extractDiff(process));
            var exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(
                        "Выполнение команды GIT '" + gitDiffCommand + "' завершилось неудачно с кодом: " + exitCode
                );
            }
            return diff.get(1, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Ошибка при выполнении команды GIT '" + gitDiffCommand + "'", e);
        }
    }

    private Set<String> extractDiff(Process process) {
        var srcPackagePath = scrPackagePath();
        return new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines()
                .filter(line -> line.endsWith(".java"))
                .map(sourceCodePath -> sourceCodePath.replace(".java", ""))
                .map(sourceCodePath -> pathToClass(srcPackagePath, sourceCodePath))
                .collect(Collectors.toSet());
    }

    private String pathToClass(String srcPackage, String sourceCodePath) {
        return Paths.get(srcPackage)
                .relativize(Paths.get(sourceCodePath))
                .toString()
                .replace(File.separatorChar, '.');
    }

    private String scrPackagePath() {
        var baseDir = Paths.get(project.getBasedir().getAbsolutePath());
        var sourceDir = Paths.get(project.getBuild().getSourceDirectory());
        return baseDir.relativize(sourceDir).toString();
    }
}
