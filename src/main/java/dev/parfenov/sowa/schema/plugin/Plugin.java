package dev.parfenov.sowa.schema.plugin;

import dev.parfenov.sowa.schema.plugin.classloader.ClassLoader;
import dev.parfenov.sowa.schema.plugin.config.*;
import dev.parfenov.sowa.schema.plugin.exporters.DirectoriesBuilder;
import dev.parfenov.sowa.schema.plugin.exporters.infra.InfraExporter;
import dev.parfenov.sowa.schema.plugin.exporters.schemas.SchemaExporter;
import dev.parfenov.sowa.schema.plugin.generators.GeneratorStrategy;
import dev.parfenov.sowa.schema.plugin.generators.sowa.SowaSchemaBuilder;
import dev.parfenov.sowa.schema.plugin.parsers.ClassParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import io.github.classgraph.ScanResult;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

/**
 * Основной класс плагина для обработки REST контроллеров и генерации схем.
 * Обеспечивает объединение всех компонентов системы: парсинга, генерации и экспорта.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-10
 */
public class Plugin {

    private final ClassParserConfig parserConfig;
    private final GeneratorConfig generatorConfig;
    private final DirectoriesBuilder directoriesBuilder;
    private final InfraConfig infraConfig;
    private final Log log;

    /**
     * Создает экземпляр плагина с необходимыми конфигурациями.
     *
     * @param parserConfig       конфигурация парсера классов
     * @param generatorConfig    конфигурация генератора схем
     * @param directoriesBuilder построитель директорий
     * @param infraConfig        конфигурация инфраструктуры
     * @param log                логгер Maven
     */
    public Plugin(final ClassParserConfig parserConfig,
                  final GeneratorConfig generatorConfig,
                  final DirectoriesBuilder directoriesBuilder,
                  final InfraConfig infraConfig,
                  final Log log) {
        this.parserConfig = parserConfig;
        this.generatorConfig = generatorConfig;
        this.directoriesBuilder = directoriesBuilder;
        this.infraConfig = infraConfig;
        this.log = log;
    }

    /**
     * Запускает процесс обработки: парсинг, генерацию схем и экспорт.
     * Основной метод выполнения плагина.
     */
    public void start() {
        try (var scanResult = new ClassLoader(parserConfig).getClassgraph().scan()) {
            var restControllers = parseRestControllers(scanResult);
            generateSchemas(restControllers);
            exportResults(restControllers);
        } catch (Exception e) {
            throw new PluginExecutionException("Ошибка во время сканирования графа классов", e);
        }
    }

    /**
     * Парсит REST контроллеры из результатов сканирования.
     *
     * @param scanResult результаты сканирования классов
     * @return список обработанных REST контроллеров
     */
    private List<ClassModel> parseRestControllers(ScanResult scanResult) {
        return new ClassParser(parserConfig).parseAllRestClasses(scanResult);
    }

    /**
     * Генерирует JSON схемы для REST контроллеров.
     *
     * @param restControllers список REST контроллеров
     */
    private void generateSchemas(List<ClassModel> restControllers) {
        new SowaSchemaBuilder(GeneratorStrategy.getGenerator(generatorConfig)).setSowaSchemas(restControllers);
    }

    /**
     * Экспортирует сгенерированные схемы и инфраструктуру.
     *
     * @param restControllers список REST контроллеров с сгенерированными схемами
     */
    private void exportResults(List<ClassModel> restControllers) {
        new SchemaExporter(new ExportConfig(directoriesBuilder, log)).export(restControllers);
        new InfraExporter(infraConfig).export(restControllers);
    }

    /**
     * Исключение, выбрасываемое при ошибках выполнения плагина.
     */
    public static class PluginExecutionException extends RuntimeException {
        public PluginExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}