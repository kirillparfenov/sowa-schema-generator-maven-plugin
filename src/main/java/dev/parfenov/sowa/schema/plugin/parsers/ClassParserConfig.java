package dev.parfenov.sowa.schema.plugin.parsers;

import org.apache.maven.project.MavenProject;

/**
 * Конфигурация для парсера классов.
 *
 * @param project             Maven проект
 * @param projectBasePackages базовые пакеты проекта для сканирования
 * @param onlyGitDiff         флаг для обработки только измененных в git файлов
 * @param branchDiffWith      ветка для сравнения git diff
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public record ClassParserConfig(
        MavenProject project,
        String[] projectBasePackages,
        boolean onlyGitDiff,
        String branchDiffWith
) {
}
