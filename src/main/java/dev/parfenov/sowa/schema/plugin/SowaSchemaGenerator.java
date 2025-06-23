package dev.parfenov.sowa.schema.plugin;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.parfenov.sowa.schema.plugin.generator.SchemaGeneratorService;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

@Mojo(name = "generateSchema", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class SowaSchemaGenerator extends AbstractMojo {

    @Parameter(property = "git.diff.command", defaultValue = "git diff main --name-only")
    private String gitDiffCommand;

//    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
//    private File classesDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private final SchemaGeneratorService schemaGeneratorService = new SchemaGeneratorService();

    private URLClassLoader classLoader;

    private URLClassLoader getClassLoader() {
        if (this.classLoader == null) {
            var urls = new ArrayList<URL>();
            for (var url : getClasspathElements(true)) {
                try {
                    urls.add(new File(url).toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            this.classLoader = new URLClassLoader(
                    urls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader()
            );
        }
        return this.classLoader;
    }

    private Collection<String> getClasspathElements(boolean withRuntimeDependencies) {
        Collection<String> classpathElements;

        if (withRuntimeDependencies) {
            try {
                classpathElements = project.getRuntimeClasspathElements(); //если нужно зависимости из других библиотек
            } catch (DependencyResolutionRequiredException e) {
                throw new RuntimeException(e);
            }
        } else {
            classpathElements = Collections.singleton(project.getBuild().getOutputDirectory());
        }

        return classpathElements;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<ObjectNode> returnTypesSchemas = new ArrayList<>();
        List<ObjectNode> parameterTypeSchemas = new ArrayList<>();

        ClassGraph classGraph = new ClassGraph()
                .overrideClasspath(getClasspathElements(true))
                .enableAllInfo()
                .enableAnnotationInfo();

        try (ScanResult scanResult = classGraph.scan()) {
            for (var classInfo : scanResult.getAllClasses()) {
                if (!classInfo.hasAnnotation(RestController.class)) continue;

                var restClass = getClassLoader().loadClass(classInfo.getName());
                var restClassType = schemaGeneratorService.resolveErasedType(restClass);
                var restClassMethodsInfo = schemaGeneratorService.resolveTypeMembers(restClassType);
                returnTypesSchemas = schemaGeneratorService.generateEachReturnType(restClassMethodsInfo.getMemberMethods());


                //todo доделать код ниже - генерация для @RequestBody
                for (var methodInfo : classInfo.getMethodInfo()) {
                    var methodSignature = methodInfo.getTypeSignatureOrTypeDescriptor().toString();
                    getLog().info("methodSignature " + methodSignature);

                    for (var paramInfo : methodInfo.getParameterInfo()) {
                        if (paramInfo.hasAnnotation(RequestBody.class)) {
                            var paramTypeName = paramInfo.getTypeSignatureOrTypeDescriptor().toString();
                            getLog().info("paramTypeName: " + paramTypeName);

                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        returnTypesSchemas.forEach(schema -> {
            getLog().info("SCHEMAS GENERATED " + schema);
        });

        var diff = getDiff(gitDiffCommand);
//        getLog().info("Diff is: ");
//        diff.forEach(d -> getLog().info(d));
    }

    private List<String> getDiff(String command) throws MojoExecutionException {
        try {
            var diff = new ArrayList<String>();

            var process = Runtime.getRuntime().exec(command);
            Executors.newSingleThreadExecutor().submit(() ->
                    new BufferedReader(new InputStreamReader(process.getInputStream()))
                            .lines()
                            .filter(line -> line.endsWith(".java"))
                            .forEach(diff::add)
            );
            var exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new MojoExecutionException("Execution of command '" + command + "' failed with exit code: " + exitCode);
            }

            // return the output
            return diff;

        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Execution of command '" + command + "' failed", e);
        }
    }
}
