package dev.parfenov.sowa.schema.plugin.git;

import com.fasterxml.classmate.ResolvedType;
import dev.parfenov.sowa.schema.plugin.classloader.ClassLoader;
import dev.parfenov.sowa.schema.plugin.parsers.TypesParser;
import dev.parfenov.sowa.schema.plugin.parsers.classes.dto.RestClass;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class GitDiffParser {
    private final Git git;
    private final TypesParser typesParser = new TypesParser();
    private final ClassLoader classLoader;

    public GitDiffParser(final String branchDiffWith, final ClassLoader classLoader) {
        this.git = new Git(branchDiffWith);
        this.classLoader = classLoader;
    }

    public void setNullForNoDiff(List<RestClass> parsedClasses) {
        if (CollectionUtils.isEmpty(parsedClasses)) return;

        try (var scanResult = classLoader.getClassgraph().scan()) {
            var sourceDependencies = scanResult.getClassDependencyMap()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            k -> k.getKey().getSourceFile(),
                            v -> v.getValue().stream().map(ClassInfo::getSourceFile).collect(Collectors.toSet()),
                            (before, current) -> {
                                Set<String> set = new HashSet<>();
                                set.addAll(before);
                                set.addAll(current);
                                return set;
                            }
                    ));

            System.out.println("gitDiff: " + git.getDiff());

            for (var restClass : parsedClasses) {
                if (CollectionUtils.isEmpty(restClass.getMethods())) continue;

                for (var method : restClass.getMethods()) {
                    var responseRootClasses = rootClasses(method.getResponse());
                    var responseHasDiff = searchDiff(responseRootClasses, scanResult, sourceDependencies);

                    var requestRootClasses = rootClasses(method.getRequest());
                    var requestHasDiff = searchDiff(requestRootClasses, scanResult, sourceDependencies);

                    if (responseHasDiff || requestHasDiff) {
                        var res = responseHasDiff ? "response: " + responseRootClasses : "request: " + requestRootClasses;
                        System.out.println("Построить схему для метода " + method.getName() + " для " + res);
                    }
                    if (!responseHasDiff) {
                        method.setResponse(null);
                    }
                    if (!requestHasDiff) {
                        method.setRequest(null);
                    }
                }
            }
        }
    }

    private Set<Class<?>> rootClasses(Type root) {
        if (root == null) return null;
        var resolvedRoot = typesParser.resolveErasedType(root);
        var rootClasses = new HashSet<Class<?>>();
        extract(resolvedRoot, rootClasses);
        return rootClasses;
    }

    private void extract(ResolvedType type, Set<Class<?>> rootClasses) {
        rootClasses.add(type.getErasedType());
        for (ResolvedType param : type.getTypeParameters()) {
            extract(param, rootClasses);
        }
    }

    private boolean searchDiff(
            Set<Class<?>> rootClasses,
            ScanResult scanResult,
            Map<String, Set<String>> sourceDependencies
    ) {
        if (CollectionUtils.isEmpty(rootClasses)) return false;

        var diff = git.getDiff();

        for (var rootClass : rootClasses) {
            var sourceFile = Optional
                    .ofNullable(scanResult.getClassInfo(rootClass.getName()))
                    .map(ClassInfo::getSourceFile)
                    .orElse("");
            if (sourceFile.isBlank()) continue;
            var hasDiff = recursiveSearchDiff(diff, sourceFile, sourceDependencies, new HashSet<>());
            if (hasDiff) return true;
        }
        return false;
    }

    private boolean recursiveSearchDiff(
            Set<String> diff,
            String sourceFile,
            Map<String, Set<String>> sourceDependencies,
            Set<String> visitedSourceFiles
    ) {
        if (visitedSourceFiles.contains(sourceFile)) return false;
        visitedSourceFiles.add(sourceFile);

        if (CollectionUtils.isEmpty(diff)) return false;
        if (diff.contains(sourceFile)) return true;

        if (!sourceDependencies.containsKey(sourceFile)) return false;
        for (var source : sourceDependencies.get(sourceFile)) {
            if (recursiveSearchDiff(diff, source, sourceDependencies, visitedSourceFiles)) {
                return true;
            }
        }

        return false;
    }
}
