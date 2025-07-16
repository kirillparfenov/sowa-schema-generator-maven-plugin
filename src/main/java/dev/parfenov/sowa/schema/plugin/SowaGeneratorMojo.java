package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.exporter.infra.InfraConfig;
import dev.parfenov.sowa.schema.plugin.exporter.infra.InfraExporter;
import dev.parfenov.sowa.schema.plugin.exporter.schemas.ExportConfig;
import dev.parfenov.sowa.schema.plugin.exporter.schemas.SchemaExporter;
import dev.parfenov.sowa.schema.plugin.generator.GeneratorConfig;
import dev.parfenov.sowa.schema.plugin.generator.GeneratorStrategy;
import dev.parfenov.sowa.schema.plugin.git.Git;
import dev.parfenov.sowa.schema.plugin.parsers.classes.ClassParser;
import dev.parfenov.sowa.schema.plugin.parsers.classes.ClassParserConfig;
import dev.parfenov.sowa.schema.plugin.sowa.SowaSchemaGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.util.Optional;

@Mojo(name = "generateSchema",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        threadSafe = true
)
public class SowaGeneratorMojo extends AbstractMojo {

    @Parameter(property = "sowaProfileName", defaultValue = "SOWA_PROFILE_NAME")
    private String sowaProfileName;

    @Parameter(property = "branchDiffWith", defaultValue = "origin/develop")
    private String branchDiffWith;

    @Parameter(property = "onlyGitDiff", defaultValue = "false")
    private boolean onlyGitDiff;

    @Parameter(property = "extractDefinitions", required = true)
    private boolean extractDefinitions;

    @Parameter(property = "stringLengthIncreasePercent")
    private int stringLengthIncreasePercent;

    @Parameter(property = "projectPackage", required = true)
    private String projectPackage;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public synchronized void execute() {
        // Парсинг классов и методов
        var parserConfig = new ClassParserConfig(project, projectPackage);
        var restControllers = new ClassParser(parserConfig).parseAllRestClasses();

        // Генерация схем
        var generator = GeneratorStrategy.getGenerator(new GeneratorConfig(extractDefinitions, stringLengthIncreasePercent));
        var sowaSchemas = new SowaSchemaGenerator(generator, project).generateSchema(restControllers);

        // Git diff
        Optional<Git> git = onlyGitDiff ? Optional.of(new Git(branchDiffWith, project)) : Optional.empty();
        var gitDiff = git.map(Git::getDiff).orElse(null);

        // Экспорт
        // Схем
        new SchemaExporter(new ExportConfig(project, getLog(), gitDiff)).export(sowaSchemas);
        // Инфры
        new InfraExporter(new InfraConfig(project, sowaProfileName, gitDiff)).export(restControllers);
    }
}
