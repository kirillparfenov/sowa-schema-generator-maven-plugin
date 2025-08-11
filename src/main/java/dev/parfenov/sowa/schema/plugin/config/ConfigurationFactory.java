package dev.parfenov.sowa.schema.plugin.config;

import dev.parfenov.sowa.schema.plugin.Plugin;
import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import dev.parfenov.sowa.schema.plugin.parsers.PropertiesParser;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.gradle.api.Project;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Arrays;

/**
 * Фабрика для централизованного создания конфигураций плагина.
 * Предоставляет единое место для создания всех типов конфигураций,
 * используемых в Plugin и SowaGeneratorMojo.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-10
 */
public final class ConfigurationFactory {

    private static final String DEFAULT_BRANCH_DIFF = "origin/main";
    private static final String DEFAULT_SOWA_PROFILE = "SOWA_PROFILE_NAME";

    private ConfigurationFactory() {
    }

    /**
     * Создает конфигурацию парсера классов для Maven контекста.
     *
     * @param project         Maven проект
     * @param projectPackages пакеты для сканирования
     * @param onlyGitDiff     флаг обработки только git изменений
     * @param branchDiffWith  ветка для сравнения
     * @param contextPath     контекстный путь сервлета
     * @return конфигурация парсера
     */
    public static ClassParserConfig createMavenParserConfig(MavenProject project,
                                                            String[] projectPackages,
                                                            boolean onlyGitDiff,
                                                            String branchDiffWith,
                                                            String contextPath) {
        return new ClassParserConfig(
                project,
                null,
                projectPackages,
                onlyGitDiff,
                branchDiffWith,
                null,
                contextPath
        );
    }

    /**
     * Создает конфигурацию парсера классов для автономного запуска.
     *
     * @param packages       пакеты для сканирования
     * @param onlyGitDiff    флаг обработки только git изменений
     * @param branchDiffWith ветка для сравнения
     * @param uberJarPath    путь к uber JAR файлу
     * @param contextPath    контекстный путь сервлета
     * @return конфигурация парсера
     */
    public static ClassParserConfig createStandaloneParserConfig(String[] packages,
                                                                 boolean onlyGitDiff,
                                                                 String branchDiffWith,
                                                                 String uberJarPath,
                                                                 String contextPath) {
        return new ClassParserConfig(
                null,
                null,
                packages,
                onlyGitDiff,
                branchDiffWith,
                uberJarPath,
                contextPath
        );
    }

    /**
     * Создает конфигурацию генератора схем.
     *
     * @param extractDefinitions          флаг извлечения определений
     * @param stringLengthIncreasePercent процент увеличения длины строк
     * @return конфигурация генератора
     */
    public static GeneratorConfig createGeneratorConfig(boolean extractDefinitions,
                                                        int stringLengthIncreasePercent) {
        return new GeneratorConfig(extractDefinitions, stringLengthIncreasePercent);
    }

    /**
     * Создает конфигурацию инфраструктуры.
     *
     * @param directoriesBuilder построитель директорий
     * @param sowaProfileName    имя профиля Sowa
     * @param contextPath        контекстный путь сервлета
     * @return конфигурация инфраструктуры
     */
    public static InfraConfig createInfraConfig(DirectoriesBuilder directoriesBuilder,
                                                String sowaProfileName,
                                                String contextPath) {
        return new InfraConfig(directoriesBuilder, sowaProfileName, contextPath);
    }

    /**
     * Создает экземпляр Plugin для Maven контекста.
     *
     * @param project                     Maven проект
     * @param projectPackages             пакеты для сканирования
     * @param onlyGitDiff                 флаг обработки только git изменений
     * @param branchDiffWith              ветка для сравнения
     * @param extractDefinitions          флаг извлечения определений
     * @param stringLengthIncreasePercent процент увеличения длины строк
     * @param sowaProfileName             имя профиля Sowa
     * @param log                         логгер Maven
     * @return настроенный экземпляр Plugin
     */
    public static Plugin createMavenPlugin(MavenProject project,
                                           String[] projectPackages,
                                           boolean onlyGitDiff,
                                           String branchDiffWith,
                                           boolean extractDefinitions,
                                           int stringLengthIncreasePercent,
                                           String sowaProfileName,
                                           Log log) {
        var contextPath = PropertiesParser.contextPath(project.getBasedir());

        var parserConfig = createMavenParserConfig(
                project, projectPackages, onlyGitDiff, branchDiffWith, contextPath
        );

        var generatorConfig = createGeneratorConfig(extractDefinitions, stringLengthIncreasePercent);
        var directoriesBuilder = new DirectoriesBuilder(new File(project.getBuild().getDirectory()));
        var infraConfig = createInfraConfig(directoriesBuilder, sowaProfileName, contextPath);

        return new Plugin(parserConfig, generatorConfig, directoriesBuilder, infraConfig, log);
    }

    /**
     * Создает экземпляр Plugin для Gradle контекста.
     *
     * @param project                     Gradle проект
     * @param projectPackages             пакеты для сканирования
     * @param onlyGitDiff                 флаг обработки только git изменений
     * @param branchDiffWith              ветка для сравнения
     * @param extractDefinitions          флаг извлечения определений
     * @param stringLengthIncreasePercent процент увеличения длины строк
     * @param sowaProfileName             имя профиля Sowa
     * @param log                         логгер Gradle
     * @return настроенный экземпляр Plugin
     */
    public static Plugin createGradlePlugin(Project project,
                                            String[] projectPackages,
                                            boolean onlyGitDiff,
                                            String branchDiffWith,
                                            boolean extractDefinitions,
                                            int stringLengthIncreasePercent,
                                            String sowaProfileName,
                                            Log log) {
        var contextPath = PropertiesParser.contextPath(project.getProjectDir());

        var parserConfig = createGradleParserConfig(
                project, projectPackages, onlyGitDiff, branchDiffWith, contextPath
        );

        var generatorConfig = createGeneratorConfig(extractDefinitions, stringLengthIncreasePercent);
        var directoriesBuilder = new DirectoriesBuilder(project.getBuildDir());
        var infraConfig = createGradleInfraConfig(directoriesBuilder, sowaProfileName, contextPath);

        return new Plugin(parserConfig, generatorConfig, directoriesBuilder, infraConfig, log);
    }

    /**
     * Создает конфигурацию парсера классов для Gradle контекста.
     *
     * @param project         Gradle проект
     * @param projectPackages пакеты для сканирования
     * @param onlyGitDiff     флаг обработки только git изменений
     * @param branchDiffWith  ветка для сравнения
     * @param contextPath     контекстный путь сервлета
     * @return конфигурация парсера
     */
    private static ClassParserConfig createGradleParserConfig(Project project,
                                                              String[] projectPackages,
                                                              boolean onlyGitDiff,
                                                              String branchDiffWith,
                                                              String contextPath) {
        return new ClassParserConfig(
                null,
                project,
                projectPackages,
                onlyGitDiff,
                branchDiffWith,
                null,
                contextPath
        );
    }

    /**
     * Создает конфигурацию инфраструктуры для Gradle контекста.
     *
     * @param directoriesBuilder построитель директорий
     * @param sowaProfileName    имя профиля Sowa
     * @param contextPath        контекстный путь сервлета
     * @return конфигурация инфраструктуры
     */
    private static InfraConfig createGradleInfraConfig(DirectoriesBuilder directoriesBuilder,
                                                       String sowaProfileName,
                                                       String contextPath) {
        return new InfraConfig(directoriesBuilder, sowaProfileName, contextPath);
    }

    /**
     * Создает экземпляр Plugin для автономного запуска.
     *
     * @param packages                    пакеты для сканирования
     * @param onlyGitDiff                 флаг обработки только git изменений
     * @param branchDiffWith              ветка для сравнения
     * @param extractDefinitions          флаг извлечения определений
     * @param stringLengthIncreasePercent процент увеличения длины строк
     * @param sowaProfileName             имя профиля Sowa
     * @param uberJarPath                 путь к uber JAR файлу
     * @return настроенный экземпляр Plugin
     */
    public static Plugin createCliPlugin(String[] packages,
                                         boolean onlyGitDiff,
                                         String branchDiffWith,
                                         boolean extractDefinitions,
                                         int stringLengthIncreasePercent,
                                         String sowaProfileName,
                                         String uberJarPath) {
        var contextPath = PropertiesParser.contextPath(uberJarPath);

        var parserConfig = createStandaloneParserConfig(
                packages, onlyGitDiff, branchDiffWith, uberJarPath, contextPath
        );

        var generatorConfig = createGeneratorConfig(extractDefinitions, stringLengthIncreasePercent);
        var directoriesBuilder = new DirectoriesBuilder(new File("sowa-build"));
        var infraConfig = createInfraConfig(directoriesBuilder, sowaProfileName, contextPath);

        return new Plugin(parserConfig, generatorConfig, directoriesBuilder, infraConfig, null);
    }

    /**
     * Создает экземпляр Plugin для автономного запуска, загружая конфигурацию из системных свойств.
     * Также логирует загруженную конфигурацию в консоль.
     *
     * @return настроенный экземпляр Plugin
     */
    public static Plugin createCliPluginFromSystemProperties() {
        var onlyGitDiff = Boolean.parseBoolean(System.getProperty("onlyGitDiff", "false"));
        var branchDiffWith = System.getProperty("branchDiffWith", DEFAULT_BRANCH_DIFF);
        var extractDefinitions = Boolean.parseBoolean(System.getProperty("extractDefinitions", "false"));
        var stringLengthIncreasePercent = Integer.parseInt(System.getProperty("stringLengthIncreasePercent", "0"));
        var sowaProfileName = System.getProperty("sowaProfileName", DEFAULT_SOWA_PROFILE);

        var uberJarPath = System.getProperty("uberJarPath");
        Assert.hasText(uberJarPath, "Не передан параметр -DuberJarLink=<ссылка на uber-jar>");

        var projectPackages = System.getProperty("projectPackages");
        Assert.hasText(projectPackages, "Не передан параметр -Dpackages=[com.example.package,common.package]");
        var packages = Arrays
                .stream(projectPackages.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        // Логируем загруженную конфигурацию
        System.out.println("PACKAGES: " + java.util.Arrays.toString(packages));
        System.out.println("ONLY_GIT_DIFF: " + onlyGitDiff);
        System.out.println("BRANCH_DIFF: " + branchDiffWith);
        System.out.println("EXTRACT_DEFINITIONS: " + extractDefinitions);
        System.out.println("STRING LENGTH INCREASE: " + stringLengthIncreasePercent);
        System.out.println("SOWA_PROFILE_NAME: " + sowaProfileName);
        System.out.println("UBER_JAR_LINK: " + uberJarPath);

        return createCliPlugin(
                packages,
                onlyGitDiff,
                branchDiffWith,
                extractDefinitions,
                stringLengthIncreasePercent,
                sowaProfileName,
                uberJarPath
        );
    }
}