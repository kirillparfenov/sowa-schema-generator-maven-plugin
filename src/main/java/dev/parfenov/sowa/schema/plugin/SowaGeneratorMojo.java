package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.exporter.infra.InfraConfig;
import dev.parfenov.sowa.schema.plugin.exporter.infra.InfraExporterImpl;
import dev.parfenov.sowa.schema.plugin.exporter.schemas.ExportConfig;
import dev.parfenov.sowa.schema.plugin.exporter.schemas.ExportStrategy;
import dev.parfenov.sowa.schema.plugin.generator.Generator;
import dev.parfenov.sowa.schema.plugin.generator.GeneratorConfig;
import dev.parfenov.sowa.schema.plugin.parsers.classes.ClassParserConfig;
import dev.parfenov.sowa.schema.plugin.parsers.classes.ClassParserStrategy;
import dev.parfenov.sowa.schema.plugin.sowa.SowaSchemaGeneratorImpl;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generateSchema",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        threadSafe = true
)
public class SowaGeneratorMojo extends AbstractMojo {

    @Parameter(property = "sowaProfileName", defaultValue = "SOWA_PROFILE_NAME")
    private String sowaProfileName;

    @Parameter(property = "gitDiffCommand", defaultValue = "git diff main --name-only")
    private String gitDiffCommand;

    @Parameter(property = "onlyGitDiff", defaultValue = "false")
    private boolean onlyGitDiff;

    @Parameter(property = "projectPackage", required = true)
    private String projectPackage;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public synchronized void execute() {
        // Парсинг классов и методов
        var parserConfig = new ClassParserConfig(onlyGitDiff, gitDiffCommand, project, getLog(), projectPackage);
        var classParser = ClassParserStrategy.getClassParser(parserConfig);
        var restControllers = classParser.parseAllRestClasses();

        // Генерация схем
        var generatorConfig = new GeneratorConfig();
        var generator = new Generator(generatorConfig);
        var sowaSchemaGenerator = new SowaSchemaGeneratorImpl(generator, project);
        var sowaSchemas = sowaSchemaGenerator.generateSchema(restControllers);

        // Экспорт
        var exportConfig = ExportConfig.toTarget(project, getLog());
        var exporter = ExportStrategy.getExporter(exportConfig);
        exporter.ifPresent(e -> e.export(sowaSchemas));
        var infraExporter = new InfraExporterImpl(new InfraConfig(project, sowaProfileName));
        infraExporter.export(restControllers);
    }
}
