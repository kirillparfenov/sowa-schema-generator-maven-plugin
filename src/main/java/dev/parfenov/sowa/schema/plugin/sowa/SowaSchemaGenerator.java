/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.sowa;

import dev.parfenov.sowa.schema.plugin.generator.dto.GeneratedResult;
import dev.parfenov.sowa.schema.plugin.generator.Generator;
import dev.parfenov.sowa.schema.plugin.parsers.EndpointPathParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;
import org.apache.maven.project.MavenProject;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Генератор схем Sowa из REST контроллеров.
 * <p>
 * Преобразует информацию о REST классах и их методах в схемы Sowa,
 * генерируя JSON Schema для типов запросов и ответов.
 */
public class SowaSchemaGenerator {

    private static final String REQUEST_SUFFIX = "_request";
    private static final String RESPONSE_SUFFIX = "_response";

    private final Generator generator;
    private final EndpointPathParser pathResolver;

    /**
     * Создает генератор схем Sowa.
     *
     * @param generator    генератор JSON Schema
     * @param mavenProject Maven проект для разрешения путей
     */
    public SowaSchemaGenerator(final Generator generator,
                               final MavenProject mavenProject) {
        this.generator = generator;
        this.pathResolver = new EndpointPathParser(mavenProject);
    }

    /**
     * Генерирует схемы Sowa для списка REST классов.
     *
     * @param restClasses список REST контроллеров для обработки
     * @return список сгенерированных схем Sowa
     */
    public List<SowaSchema> generateSchema(List<RestClass> restClasses) {
        if (CollectionUtils.isEmpty(restClasses)) {
            return List.of();
        }
        try {
            return restClasses
                    .stream()
                    .map(this::generate)
                    .flatMap(List::stream)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время генерации схемы из " + restClasses, e);
        }
    }

    /**
     * Генерирует схемы для одного REST класса.
     *
     * @param restClass REST контроллер для обработки
     * @return список схем для всех методов контроллера
     */
    private List<SowaSchema> generate(RestClass restClass) {
        var schemas = new ArrayList<SowaSchema>();
        for (var method : restClass.getMethods()) {
            var sowaSchema = new SowaSchema();
            setNames(sowaSchema, restClass, method);
            setRequestResponse(sowaSchema, restClass, method);
            setPath(sowaSchema, restClass, method);
            schemas.add(sowaSchema);
        }
        return schemas;
    }

    /**
     * Устанавливает имена класса и метода в схему.
     *
     * @param sowaSchema схема для заполнения
     * @param restClass  REST контроллер
     * @param method     метод контроллера
     */
    private void setNames(SowaSchema sowaSchema, RestClass restClass, RestMethod method) {
        sowaSchema.setRestClassName(restClass.getName());
        sowaSchema.setRestMethodName(method.getName());
    }

    /**
     * Генерирует и устанавливает схемы запроса и ответа.
     *
     * @param sowaSchema схема для заполнения
     * @param restClass  REST контроллер
     * @param method     метод контроллера
     */
    private void setRequestResponse(SowaSchema sowaSchema, RestClass restClass, RestMethod method) {
        var schemaName = pathResolver.schemaName(restClass, method);
        var request = generate(method.getRequest(), schemaName.concat(REQUEST_SUFFIX));
        var response = generate(method.getResponse(), schemaName.concat(RESPONSE_SUFFIX));
        sowaSchema.setRequest(request);
        sowaSchema.setResponse(response);
    }

    /**
     * Генерирует JSON Schema для указанного типа.
     *
     * @param type       тип для генерации схемы
     * @param schemaName имя схемы
     * @return результат генерации или null если тип не указан
     */
    private GeneratedResult generate(Type type, String schemaName) {
        return type == null ? null : generator.generate(type, schemaName);
    }

    /**
     * Устанавливает информацию о пути эндпоинта.
     *
     * @param sowaSchema схема для заполнения
     * @param restClass  REST контроллер
     * @param method     метод контроллера
     */
    private void setPath(SowaSchema sowaSchema, RestClass restClass, RestMethod method) {
        sowaSchema.setPathVariables(method.getPathVariables());
        sowaSchema.setFullEndpointPath(restClass.getEndpointPath() + method.getEndpointPath());
    }
}
