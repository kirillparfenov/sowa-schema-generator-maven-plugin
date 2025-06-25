package dev.parfenov.sowa.schema.plugin.classloader;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ClassLoader {

    private final MavenProject project;
    private URLClassLoader classLoader;

    public ClassLoader(MavenProject project) {
        this.project = project;
    }

    /**
     * Получить ClassLoader
     *
     * @return ClassLoader
     */
    public URLClassLoader getClassLoader() {
        //todo synchronized через lock (для 21 java)
        if (this.classLoader == null) {
            var urls = new ArrayList<URL>();
            for (var url : getClasspathElements()) {
                try {
                    urls.add(new File(url).toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Ошибка создания URL ", e);
                }
            }
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

    /**
     * Сканирует classpath
     *
     * @return результат сканирования classpath
     */
    public ScanResult scanClasspath() {
        ClassGraph classGraph = new ClassGraph()
                .overrideClasspath(getClasspathElements())
                .enableAllInfo();

        return classGraph.scan();
    }

    private Collection<String> getClasspathElements() {
        //classpathElements = project.getRuntimeClasspathElements(); //если нужно зависимости из других библиотек
        return Collections.singleton(project.getBuild().getOutputDirectory());
    }
}
