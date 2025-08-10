package dev.parfenov.sowa.schema.plugin.config.gradle;

import dev.parfenov.sowa.schema.plugin.config.ConfigurationFactory;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

/**
 * Gradle Task для генерации JSON Schema из Spring REST контроллеров.
 * Выполняет ту же функциональность что и Maven plugin, но в контексте Gradle.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-10
 */
public class GenerateSchemaTask extends DefaultTask {

    @Inject
    public GenerateSchemaTask() {
        setDescription("Генерирует JSON Schema из Spring REST контроллеров в формате Sowa");
        setGroup("sowa");

        // Задача должна выполняться после компиляции
        dependsOn("compileJava");
        dependsOn("processResources");
        dependsOn("classes");
    }

    /**
     * Основной метод выполнения задачи.
     * Загружает конфигурацию из extension и выполняет генерацию схем.
     */
    @TaskAction
    public void generateSchema() {
        try {
            var extension = getProject().getExtensions().getByType(SowaSchemaExtension.class);
            validateConfiguration(extension);
            ConfigurationFactory.createGradlePlugin(
                    getProject(),
                    extension.getProjectPackages(),
                    extension.isOnlyGitDiff(),
                    extension.getBranchDiffWith(),
                    extension.isExtractDefinitions(),
                    extension.getStringLengthIncreasePercent(),
                    extension.getSowaProfileName(),
                    null
            ).start();
        } catch (Exception e) {
            System.out.println("Ошибка при генерации схем: " + e.getMessage());
            throw new GradleException("Не удалось сгенерировать схемы", e);
        }
    }

    /**
     * Валидирует конфигурацию extension.
     */
    private void validateConfiguration(SowaSchemaExtension extension) {
        if (extension.getProjectPackages() == null || extension.getProjectPackages().length == 0) {
            throw new GradleException(
                    "Не указаны пакеты для сканирования. Установите свойство projectPackages в конфигурации sowaSchema"
            );
        }
    }
}