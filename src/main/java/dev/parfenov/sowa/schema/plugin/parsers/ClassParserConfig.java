/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.parsers;

import org.apache.maven.project.MavenProject;

/**
 * Конфигурация для парсера классов.
 *
 * @param project            Maven проект
 * @param projectBasePackage базовый пакет проекта для сканирования
 * @param onlyGitDiff        флаг для обработки только измененных в git файлов
 * @param branchDiffWith     ветка для сравнения git diff
 */
public record ClassParserConfig(
        MavenProject project,
        String projectBasePackage,
        boolean onlyGitDiff,
        String branchDiffWith
) {
}
