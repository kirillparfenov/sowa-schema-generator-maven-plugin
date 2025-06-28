package dev.parfenov.sowa.schema.plugin.classloader;

import io.github.classgraph.ClassGraph;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ClassLoader {

    private final MavenProject project;
    private final List<URL> classpathElements = new ArrayList<>();
    private URLClassLoader classLoader;
    private String packageName;

    public ClassLoader(MavenProject project) {
        this.project = project;
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

    /**
     * Загружает erased класс
     *
     * @param name имя класса
     * @return erased класс
     */
    public Class<?> loadErasedClass(String name) {
        try {
            return getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Ошибка во время загрузки класса", e);
        }
    }

    public String baseProjectPackage() {
        if (this.packageName != null) {
            return this.packageName;
        }
        this.packageName = ClassloaderUtils.baseProjectPackage(project);
        return this.packageName;
    }

    public ClassGraph getClassgraph() {
        return new ClassGraph()
                .overrideClasspath(getClasspathElements())
                .overrideClassLoaders(getClassLoader())
                .acceptPackages(baseProjectPackage())
                .enableAllInfo();
    }

    private List<URL> getClasspathElements() {
        if (!this.classpathElements.isEmpty()) {
            return this.classpathElements;
        }
        try {
            var classpathElements = new HashSet<String>();
            classpathElements.addAll(project.getRuntimeClasspathElements());
            classpathElements.addAll(project.getCompileClasspathElements());
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
