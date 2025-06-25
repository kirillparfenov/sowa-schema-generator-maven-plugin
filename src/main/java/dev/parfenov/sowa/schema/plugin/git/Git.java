package dev.parfenov.sowa.schema.plugin.git;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class Git {

    private final String gitDiffCommand;

    public Git(String gitDiffCommand) {
        this.gitDiffCommand = gitDiffCommand;
    }

    /**
     * Получить разницу между текущей веткой и develop
     *
     * @return список файлов с расширением .java, которые подверглись изменениями
     */
    //todo кастомизация: 1) diff относительно кастомной ветки; 2) список расширений файлов
    public List<String> getDiff() throws MojoExecutionException {
        try {
            var diff = new ArrayList<String>();
            var process = Runtime.getRuntime().exec(gitDiffCommand);
            Executors.newSingleThreadExecutor().submit(() ->
                    new BufferedReader(new InputStreamReader(process.getInputStream()))
                            .lines()
                            .filter(line -> line.endsWith(".java"))
                            .forEach(diff::add)
            );
            var exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException(
                        "Выполнение команды GIT '" + gitDiffCommand + "' завершилось неудачно с кодом: " + exitCode
                );
            }
            return diff;
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Ошибка при выполнении команды GIT '" + gitDiffCommand + "'", e);
        }
    }
}
