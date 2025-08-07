package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import dev.parfenov.sowa.schema.plugin.exporters.infra.InfraConfig;
import dev.parfenov.sowa.schema.plugin.exporters.infra.InfraExporter;
import dev.parfenov.sowa.schema.plugin.exporters.schemas.ExportConfig;
import dev.parfenov.sowa.schema.plugin.exporters.schemas.SchemaExporter;
import dev.parfenov.sowa.schema.plugin.generators.GeneratorStrategy;
import dev.parfenov.sowa.schema.plugin.generators.config.GeneratorConfig;
import dev.parfenov.sowa.schema.plugin.generators.sowa.SowaSchemaBuilder;
import dev.parfenov.sowa.schema.plugin.parsers.ClassParser;
import dev.parfenov.sowa.schema.plugin.parsers.ClassParserConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Maven плагин для генерации JSON Schema из Spring REST контроллеров.
 * <p>
 * Плагин анализирует классы REST контроллеров, извлекает информацию о типах запросов/ответов
 * и генерирует соответствующие JSON Schema файлы в формате Sowa.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
@Mojo(name = "generateSchema",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        threadSafe = true
)
public class SowaGeneratorMojo extends AbstractMojo {

    /**
     * Имя профиля Sowa для конфигурации инфраструктуры
     */
    @Parameter(property = "sowaProfileName", defaultValue = "SOWA_PROFILE_NAME")
    private String sowaProfileName;

    /**
     * Ветка для сравнения изменений при использовании git diff режима
     */
    @Parameter(property = "branchDiffWith", defaultValue = "origin/develop")
    private String branchDiffWith;

    /**
     * Флаг для обработки только измененных в git файлов
     */
    @Parameter(property = "onlyGitDiff", defaultValue = "false")
    private boolean onlyGitDiff;

    /**
     * Флаг для извлечения определений в отдельные файлы
     */
    @Parameter(property = "extractDefinitions")
    private boolean extractDefinitions;

    /**
     * Процент увеличения длины строк для валидации
     */
    @Parameter(property = "stringLengthIncreasePercent")
    private int stringLengthIncreasePercent;

    /**
     * Базовые пакеты проекта для сканирования классов
     */
    @Parameter(property = "projectPackages", required = true)
    private String[] projectPackages;

    /**
     * Объект Maven проекта
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Основной метод выполнения плагина.
     * <p>
     * Выполняет последовательность операций:
     * 1. Парсинг REST контроллеров
     * 2. Генерация JSON Schema
     * 3. Экспорт схем и инфраструктуры
     */
    @Override
    public synchronized void execute() {
        var parserConfig = new ClassParserConfig(project, projectPackages, onlyGitDiff, branchDiffWith);
        var restControllers = new ClassParser(parserConfig).parseAllRestClasses();

        var generator = GeneratorStrategy.getGenerator(new GeneratorConfig(extractDefinitions, stringLengthIncreasePercent));
        new SowaSchemaBuilder(generator).setSowaSchemas(restControllers);

        var directoriesBuilder = new DirectoriesBuilder(project);
        new SchemaExporter(new ExportConfig(directoriesBuilder, project, getLog())).export(restControllers);
        new InfraExporter(new InfraConfig(directoriesBuilder, project, sowaProfileName)).export(restControllers);
    }
}
