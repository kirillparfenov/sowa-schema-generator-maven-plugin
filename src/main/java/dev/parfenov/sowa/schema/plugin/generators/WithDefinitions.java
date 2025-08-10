package dev.parfenov.sowa.schema.plugin.generators;

import com.github.victools.jsonschema.generator.SchemaGenerator;
import dev.parfenov.sowa.schema.plugin.config.GeneratorConfig;
import dev.parfenov.sowa.schema.plugin.generators.dto.GeneratedResult;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Генератор JSON Schema со встроенными определениями.
 * <p>
 * Создает схемы где все определения включены непосредственно в основную схему,
 * без выделения их в отдельные объекты.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class WithDefinitions implements Generator {

    private final SchemaGenerator schemaGenerator;

    /**
     * Создает генератор с конфигурацией.
     *
     * @param config конфигурация генератора
     */
    public WithDefinitions(GeneratorConfig config) {
        this.schemaGenerator = new SchemaGenerator(config.getConfig());
    }

    /**
     * Генерирует JSON Schema для указанного типа.
     *
     * @param type       тип для генерации схемы
     * @param schemaName имя схемы
     * @return результат генерации с основной схемой и пустым списком определений
     */
    @Override
    public GeneratedResult generate(Type type, String schemaName) {
        return new GeneratedResult(
                schemaName,
                schemaGenerator.generateSchema(type),
                List.of()
        );
    }
}
