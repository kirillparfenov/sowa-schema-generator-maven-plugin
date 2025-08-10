package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.config.gradle.GenerateSchemaTask;
import dev.parfenov.sowa.schema.plugin.config.gradle.SowaSchemaExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle Plugin для генерации JSON Schema из Spring REST контроллеров.
 * Регистрирует extension для конфигурации и task для выполнения генерации.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-10
 */
public class SowaGradle implements Plugin<Project> {

    public static final String EXTENSION_NAME = "sowaSchema";
    public static final String TASK_NAME = "generateSchema";

    @Override
    public void apply(Project project) {
        // Создаем extension для конфигурации
        project.getExtensions().add(EXTENSION_NAME, new SowaSchemaExtension());

        // Регистрируем задачу для генерации схем
        project.getTasks().create(TASK_NAME, GenerateSchemaTask.class);
    }
}