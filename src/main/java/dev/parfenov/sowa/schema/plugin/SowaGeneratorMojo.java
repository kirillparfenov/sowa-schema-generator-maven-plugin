package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.classparser.ClassParserConfig;
import dev.parfenov.sowa.schema.plugin.classparser.ClassParserStrategy;
import dev.parfenov.sowa.schema.plugin.exporter.ExportConfig;
import dev.parfenov.sowa.schema.plugin.exporter.ExportStrategy;
import dev.parfenov.sowa.schema.plugin.generator.Generator;
import dev.parfenov.sowa.schema.plugin.generator.GeneratorConfig;
import dev.parfenov.sowa.schema.plugin.sowa.SowaSchemaGeneratorImpl;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generateSchema", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class SowaGeneratorMojo extends AbstractMojo {

    @Parameter(property = "git.diff.command", defaultValue = "git diff main --name-only")
    private String gitDiffCommand;

    @Parameter(property = "onlyGitDiff", defaultValue = "false")
    private boolean onlyGitDiff;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Парсинг классов и методов
        var parserConfig = new ClassParserConfig(onlyGitDiff, gitDiffCommand, project);
        var classParser = ClassParserStrategy.getClassParser(parserConfig);
        var restControllersMethods = classParser.getAllRestControllersMethods();

        // Генерация
        var generator = new Generator(new GeneratorConfig());
        var sowaSchemaGenerator = new SowaSchemaGeneratorImpl(generator);
        var sowaSchemas = sowaSchemaGenerator.generateSchema(restControllersMethods);

        // Экспорт
        var exportConfig = ExportConfig.toTarget(project);
        var exporter = ExportStrategy.getExporter(exportConfig);
        exporter.ifPresent(e -> e.export(sowaSchemas));
    }
}
