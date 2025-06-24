package dev.parfenov.sowa.schema.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.parfenov.sowa.schema.plugin.classparser.AllClassPathParser;
import dev.parfenov.sowa.schema.plugin.classparser.GitClassParser;
import dev.parfenov.sowa.schema.plugin.generator.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;

@Mojo(name = "generateSchema", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class SowaService extends AbstractMojo {

    @Parameter(property = "git.diff.command", defaultValue = "git diff main --name-only")
    private String gitDiffCommand;

    @Parameter(property = "onlyGitDiff", defaultValue = "false")
    private boolean onlyGitDiff;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    //    private final Git git = new Git();
//    private final SchemaGeneratorService schemaGeneratorService = new SchemaGeneratorService();
//    private final ClassLoader classLoader = new ClassLoader();
//    private final ResolvedClassLoader resolvedClassLoader = new ResolvedClassLoader();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        var classParser = onlyGitDiff ? new GitClassParser(gitDiffCommand, project) : new AllClassPathParser(project);
        var restControllersMethods = classParser.getAllMethods();
        prettyPrint(restControllersMethods);

        var schemaGenerator = new SowaSchemaGeneratorImpl(classParser);

        var returnTypesSchemas = new HashSet<RestControllerMethod>();
        var requestBodiesSchemas = new HashSet<RestControllerMethod>();

//        try (var scanResult = classLoader.scanClasspath()) {
//            var restControllers = scanResult.getClassesWithAnnotation(RestController.class);
//            for (var restController : restControllers) {
//                var restClass = classLoader.loadErasedClass(restController.getName());
//                var restClassType = schemaGeneratorService.resolveErasedType(restClass);
//                var restClassMethods = schemaGeneratorService.resolveTypeMembers(restClassType).getMemberMethods();
//                returnTypesSchemas.addAll(
//                        schemaGeneratorService.generateEachReturnType(restClassMethods)
//                );
//
//                for (var method : restClassMethods) {
//                    for (int i = 0; i < method.getArgumentCount(); i++) {
//                        for (var annotation : method.getParameterAnnotations(i)) {
//                            if (annotation.annotationType().equals(RequestBody.class)) {
//                                var schema = schemaGeneratorService.generateSchema(method.getArgumentType(i));
//                                requestBodiesSchemas.add(schema);
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("Ошибка обработки генерации схем", e);
//        }
//
//        System.out.println("print @ResponseBody");
//        returnTypesSchemas.forEach(schemaGeneratorService::prettyPrint);
//        System.out.println("print @RequestBody");
//        requestBodiesSchemas.forEach(schemaGeneratorService::prettyPrint);
    }

    void prettyPrint(List<RestControllerMethod> methods) {

        for (RestControllerMethod method : methods) {
            System.out.println(method);
        }
//        var mapper = new ObjectMapper();

//        for (var method : methods) {
//            try {
//                var json = mapper.writeValueAsString(method);
//                System.out.println(json);
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
//        }
    }
}
