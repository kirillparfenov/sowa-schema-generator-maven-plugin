package dev.parfenov.sowa.schema.plugin.classloader;

import dev.parfenov.sowa.schema.plugin.parsers.classes.ClassParserConfig;
import io.github.classgraph.ClassGraph;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ClassLoader {

    private final List<URL> classpathElements = new ArrayList<>();
    private final ClassParserConfig config;
    private URLClassLoader classLoader;

    public ClassLoader(final ClassParserConfig config) {
        this.config = config;
    }

    /**
     * Получить ClassLoader
     *
     * @return ClassLoader
     */
    public URLClassLoader getClassLoader() {
        if (this.classLoader == null) {
            var urls = getClasspathElements();
            this.classLoader = new URLClassLoader(
                    urls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader()
            );
        }
        return this.classLoader;
    }

    public String baseProjectPackage() {
        return config.projectBasePackage();
    }

    public ClassGraph getClassgraph() {
        return new ClassGraph()
                .overrideClasspath(getClasspathElements())
                .overrideClassLoaders(getClassLoader())
                .acceptPackages(baseProjectPackage())
                .enableInterClassDependencies()
                .enableAllInfo();
    }

    private List<URL> getClasspathElements() {
        if (!this.classpathElements.isEmpty()) {
            return this.classpathElements;
        }
        try {
            var classpathElements = new HashSet<String>();
            classpathElements.addAll(config.project().getRuntimeClasspathElements());
            classpathElements.addAll(config.project().getCompileClasspathElements());
            var urls = new ArrayList<URL>(classpathElements.size());
            for (var element : classpathElements) {
                urls.add(new File(element).toURI().toURL());
            }
            this.classpathElements.addAll(urls);
            return this.classpathElements;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время получения classpath элементов", e);
        }
    }
}
