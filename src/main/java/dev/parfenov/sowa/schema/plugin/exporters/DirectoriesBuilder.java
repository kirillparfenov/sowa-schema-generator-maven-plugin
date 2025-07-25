/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporters;

import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Построитель структуры директорий для экспорта схем и конфигурационных файлов.
 * Создает и управляет иерархией папок для организации выходных файлов проекта.
 *
 * <p>Структура создаваемых директорий:</p>
 * <pre>
 * target/
 *   └── sowa/
 *       ├── request/     (схемы запросов)
 *       ├── response/    (схемы ответов)
 *       └── services.yml (роуты на схемы)
 * </pre>
 */
public class DirectoriesBuilder {

    private static final String SOWA_DIRECTORY = File.separator + "sowa";
    private static final String REQUEST_DIRECTORY = File.separator + "request";
    private static final String RESPONSE_DIRECTORY = File.separator + "response";
    private static final String INFRASTRUCTURE_DIRECTORY = "";
    private static final String SERVICES_YML = "services.yml";

    private final MavenProject project;

    public DirectoriesBuilder(final MavenProject project) {
        this.project = project;
    }

    /**
     * Возвращает файл services.yml в корневой директории sowa.
     *
     * @return File services.yml
     */
    public File servicesYamlFile() {
        return new File(infraDir(), SERVICES_YML);
    }

    /**
     * Создает и возвращает директорию для схем запросов.
     * Директория создается по пути: target/sowa/request/
     *
     * @return директория для схем запросов
     */
    public File requestDir() {
        return buildDir(sowaDir(), REQUEST_DIRECTORY);
    }

    /**
     * Создает и возвращает директорию для схем ответов.
     * Директория создается по пути: target/sowa/response/
     *
     * @return директория для схем ответов
     */
    public File responseDir() {
        return buildDir(sowaDir(), RESPONSE_DIRECTORY);
    }

    /**
     * Создает объект File по указанному пути директории и имени файла.
     * Не создает физический файл, только формирует путь к нему.
     *
     * @param directory директория, в которой будет расположен файл
     * @param fileName  имя файла
     * @return объект File с полным путем к файлу
     */
    public File buildFile(File directory, String fileName) {
        return new File(directory, fileName);
    }

    /**
     * Создает и возвращает корневую директорию инфраструктуры.
     * Используется для размещения конфигурационных файлов.
     *
     * @return корневая директория инфраструктуры (sowa/)
     */
    private File infraDir() {
        return buildDir(sowaDir(), INFRASTRUCTURE_DIRECTORY);
    }

    /**
     * Создает и возвращает основную директорию sowa в target.
     * Служит корневой директорией для всех экспортируемых файлов.
     *
     * @return основная директория sowa
     */
    private File sowaDir() {
        return buildDir(new File(project.getBuild().getDirectory()), SOWA_DIRECTORY);
    }

    /**
     * Создает директорию и все необходимые родительские директории.
     * Универсальный метод для создания иерархии папок.
     *
     * @param parentDir  родительская директория
     * @param newDirName имя создаваемой директории
     * @return созданная директория
     */
    private File buildDir(File parentDir, String newDirName) {
        var dir = new File(parentDir, newDirName);
        dir.mkdirs();
        return dir;
    }
}
