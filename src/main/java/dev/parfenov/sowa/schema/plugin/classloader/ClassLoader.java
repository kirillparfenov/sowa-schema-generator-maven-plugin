package dev.parfenov.sowa.schema.plugin.classloader;

import dev.parfenov.sowa.schema.plugin.config.ClassParserConfig;
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
    public String[] baseProjectPackages() {
        return config.projectBasePackages();
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
                .acceptPackages(baseProjectPackages())
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
            if (config.project() != null) {
                classpathElements.addAll(config.project().getRuntimeClasspathElements());
                classpathElements.addAll(config.project().getCompileClasspathElements());
            } else if (config.gradleProject() != null) {
                classpathElements.addAll(getGradleClasspathElements());
            } else if (config.uberJarLink() != null) {
                classpathElements.add(config.uberJarLink());
            }
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

    /**
     * Получает все зависимости проекта (runtime + compile) для Gradle.
     *
     * @return список путей к JAR файлам зависимостей
     */
    private List<String> getGradleClasspathElements() {
        var classpathElements = new ArrayList<String>();

        try {
            // Получаем runtime classpath
            var runtimeConfiguration = config.gradleProject().getConfigurations().findByName("runtimeClasspath");
            if (runtimeConfiguration != null) {
                runtimeConfiguration.getResolvedConfiguration().getResolvedArtifacts()
                        .forEach(artifact -> {
                            String path = artifact.getFile().getAbsolutePath();
                            if (!classpathElements.contains(path)) {
                                classpathElements.add(path);
                            }
                        });
            }

            // Получаем compile classpath
            var compileConfiguration = config.gradleProject().getConfigurations().findByName("compileClasspath");
            if (compileConfiguration != null) {
                compileConfiguration.getResolvedConfiguration().getResolvedArtifacts()
                        .forEach(artifact -> {
                            String path = artifact.getFile().getAbsolutePath();
                            if (!classpathElements.contains(path)) {
                                classpathElements.add(path);
                            }
                        });
            }

            // Добавляем скомпилированные классы проекта
            var compileJavaTask = config.gradleProject().getTasks().findByName("compileJava");
            if (compileJavaTask != null) {
                compileJavaTask.getOutputs().getFiles().forEach(file -> {
                    String path = file.getAbsolutePath();
                    if (!classpathElements.contains(path)) {
                        classpathElements.add(path);
                    }
                });
            }

            // Добавляем resources директорию
            var processResourcesTask = config.gradleProject().getTasks().findByName("processResources");
            if (processResourcesTask != null) {
                processResourcesTask.getOutputs().getFiles().forEach(file -> {
                    String path = file.getAbsolutePath();
                    if (!classpathElements.contains(path)) {
                        classpathElements.add(path);
                    }
                });
            }

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении classpath элементов", e);
        }

        return classpathElements;
    }
}
