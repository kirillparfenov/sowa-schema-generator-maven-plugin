package dev.parfenov.sowa.schema.plugin.config;

import dev.parfenov.sowa.schema.plugin.Plugin;
import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import dev.parfenov.sowa.schema.plugin.parsers.PropertiesParser;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.springframework.util.Assert;

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
     * @param project            Maven проект (может быть null для автономного запуска)
     * @param sowaProfileName    имя профиля Sowa
     * @param contextPath        контекстный путь сервлета
     * @return конфигурация инфраструктуры
     */
    public static InfraConfig createInfraConfig(DirectoriesBuilder directoriesBuilder,
                                                MavenProject project,
                                                String sowaProfileName,
                                                String contextPath) {
        return new InfraConfig(directoriesBuilder, project, sowaProfileName, contextPath);
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
        var directoriesBuilder = new DirectoriesBuilder();
        var infraConfig = createInfraConfig(directoriesBuilder, project, sowaProfileName, contextPath);

        return new Plugin(parserConfig, generatorConfig, directoriesBuilder, infraConfig, log);
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
        var directoriesBuilder = new DirectoriesBuilder();
        var infraConfig = createInfraConfig(directoriesBuilder, null, sowaProfileName, contextPath);

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

        var uberJarPath = System.getProperty("uberJarLink");
        Assert.hasText(uberJarPath, "Не передан параметр -DuberJarLink=<ссылка на uber-jar>");

        var packagesParam = System.getProperty("packages");
        Assert.hasText(packagesParam, "Не передан параметр -Dpackages=[com.example.package,common.package]");
        var packages = Arrays
                .stream(packagesParam.split(","))
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