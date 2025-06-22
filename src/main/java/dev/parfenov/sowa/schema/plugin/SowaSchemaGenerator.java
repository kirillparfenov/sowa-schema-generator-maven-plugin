package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.generator.SchemaGeneratorService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Mojo(name = "generateSchema", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class SowaSchemaGenerator extends AbstractMojo {

    @Parameter(property = "git.diff.command", defaultValue = "git diff main --name-only")
    private String gitDiffCommand;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File classesDirectory;

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    private SchemaGeneratorService schemaGeneratorService = new SchemaGeneratorService();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String classesDir = classesDirectory.getAbsolutePath();
        getLog().info("Scanning classes in1: " + classesDir);


        try {
            // поиск @RestController
            var restControllers = findClassesWithAnnotation(RestController.class);
            getLog().info("RestControllers: " + restControllers);

            //todo нужна обработка Generic-типов
            //поиск @RequestBody
            var requestBodies = findClassesWithMethodParamAnnotation(restControllers, RequestBody.class);
            getLog().info("RequestBodies: " + requestBodies);

            //todo нужна обработка Generic-типов
            //поиск @ResponseBodies
            var responseBodies = findResponseBodies(restControllers);
            getLog().info("ResponseBodies: " + responseBodies);
            schemaGeneratorService.generate(responseBodies);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var diff = getDiff(gitDiffCommand);
        getLog().info("Diff is: ");
        diff.forEach(d -> getLog().info(d));
    }



    private Set<Class<?>> findClassesWithAnnotation(Class<? extends Annotation> annotation) throws Exception {
        var classes = new HashSet<Class<?>>();

        // 1. Создаём ClassLoader, который включает `target/classes`
        URL classesUrl = classesDirectory.toURI().toURL();
        ClassLoader projectClassLoader = new URLClassLoader(
                new URL[] { classesUrl },
                Thread.currentThread().getContextClassLoader() // Родительский ClassLoader
        );

        Files.walk(classesDirectory.toPath())
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(p -> {
                    try {
                        var className = convertPathToClassName(p);
                        var clazz = projectClassLoader.loadClass(className);
                        if (clazz.isAnnotationPresent(annotation)) {
                            classes.add(clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        getLog().error("Class not found: " + e);
                    } catch (NoClassDefFoundError e) {
//                        getLog().error("Class dependency missing for: " +  e); //случается, когда extends стороннюю библиотеку
                    } catch (Exception e) {
                        getLog().error("Unexpected error loading class: " +  e);
                    }
                });

        return classes;
    }

    private Set<Class<?>> findClassesWithMethodParamAnnotation(Set<Class<?>> restControllers, Class<? extends Annotation> annotation) {
        var classes = new HashSet<Class<?>>();

        restControllers.forEach(restController -> {
            for(var method : restController.getMethods()) {
                for (var param : method.getParameters()) {
                    if (param.isAnnotationPresent(annotation)) {
                        classes.add(param.getAnnotatedType().getClass());
                    }
                }
            }
        });

        return classes;
    }

    private Set<Class<?>> findResponseBodies(Set<Class<?>> restControllers) {
        var classes = new HashSet<Class<?>>();

        restControllers.forEach(restController -> {
            var noVoidMethods = Arrays.stream(restController.getDeclaredMethods())
                    .filter(method -> !method.getReturnType().equals(Void.TYPE))
                    .map(Method::getReturnType)
                    .peek(returnType -> {
                        getLog().info("Return type: " + returnType);
                        if (returnType.getPackageName().startsWith("java.")) return;
                        for (var field : returnType.getDeclaredFields()) {
                            getLog().info("Field: " + field.getName());

                            field.setAccessible(true);
                            var fieldType = field.getType();
                            getLog().info("package name: " + fieldType.getPackageName());
                        }
                    })
                    .collect(Collectors.toSet());
            classes.addAll(noVoidMethods);
        });

        return classes;
    }

    private String convertPathToClassName(Path classFilePath) {
        return classesDirectory.toPath()
                .relativize(classFilePath)
                .toString()
                .replace(File.separator, ".")
                .replace(".class", "");
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
