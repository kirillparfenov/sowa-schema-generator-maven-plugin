package dev.parfenov.sowa.schema.plugin.classloader;

import dev.parfenov.sowa.schema.plugin.parsers.ClassParserConfig;
import io.github.classgraph.ClassGraph;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Загрузчик классов для Maven проекта.
 * <p>
 * Создает ClassLoader с classpath элементами проекта и настраивает
 * ClassGraph для сканирования классов в указанном пакете.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class ClassLoader {

    private final List<URL> classpathElements = new ArrayList<>();
    private final ClassParserConfig config;
    private URLClassLoader classLoader;

    /**
     * Создает загрузчик классов с конфигурацией.
     *
     * @param config конфигурация парсера классов
     */
    public ClassLoader(final ClassParserConfig config) {
        this.config = config;
    }

    /**
     * Возвращает настроенный URLClassLoader.
     * <p>
     * Создает ClassLoader с classpath элементами проекта если он еще не создан.
     *
     * @return настроенный URLClassLoader
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
     * Возвращает базовый пакет проекта.
     *
     * @return базовый пакет проекта для сканирования
     */
    public String baseProjectPackage() {
        return config.projectBasePackage();
    }

    /**
     * Создает настроенный ClassGraph для сканирования классов.
     *
     * @return ClassGraph настроенный для сканирования пакета проекта
     */
    public ClassGraph getClassgraph() {
        return new ClassGraph()
                .overrideClasspath(getClasspathElements())
                .overrideClassLoaders(getClassLoader())
                .acceptPackages(baseProjectPackage())
                .enableInterClassDependencies()
                .enableAllInfo();
    }

    /**
     * Получает список URL classpath элементов проекта.
     *
     * @return список URL для runtime и compile classpath
     */
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
