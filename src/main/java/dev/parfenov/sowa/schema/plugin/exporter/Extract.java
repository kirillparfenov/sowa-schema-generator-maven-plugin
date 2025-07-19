/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporter;

import dev.parfenov.sowa.schema.plugin.exporter.dto.ServicesYaml;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Утилитный класс для извлечения данных из конфигураций {@link ServicesYaml}.
 * <p>
 * Содержит методы для получения:
 * <ul>
 *   <li>allowed_queries</li>
 *   <li>requests</li>
 *   <li>responses</li>
 *   <li>id</li>
 *   <li>url</li>
 * </ul>
 * Все возвращаемые коллекции неизменяемы.
 * </p>
 */
public final class Extract {

    private Extract() {
    }

    /**
     * Извлекает все разрешённые запросы из конфигураций.
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @return Список {@link ServicesYaml.AllowedQuery} (пустой, если запросы отсутствуют).
     * @throws NullPointerException если {@code services} равен {@code null}.
     */
    public static List<ServicesYaml.AllowedQuery> allowedQueries(List<ServicesYaml> services) {
        return services.stream()
                .map(ServicesYaml::getAllowedQueries)
                .filter(Predicate.not(CollectionUtils::isEmpty))
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * Извлекает валидаторы запросов из конфигураций.
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @return Список {@link ServicesYaml.RequestResponse} (пустой, если валидаторы отсутствуют).
     * @throws NullPointerException если {@code services} равен {@code null}.
     */
    public static List<ServicesYaml.RequestResponse> requests(List<ServicesYaml> services) {
        return extractRequestResponse(services, ServicesYaml.ValidatorJson::getRequest);
    }

    /**
     * Извлекает валидаторы ответов из конфигураций.
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @return Список {@link ServicesYaml.RequestResponse} (пустой, если валидаторы отсутствуют).
     * @throws NullPointerException если {@code services} равен {@code null}.
     */
    public static List<ServicesYaml.RequestResponse> responses(List<ServicesYaml> services) {
        return extractRequestResponse(services, ServicesYaml.ValidatorJson::getResponse);
    }

    /**
     * Вспомогательный метод для извлечения валидаторов (запросов или ответов).
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @param mapper   Функция-селектор ({@code getRequest} или {@code getResponse}).
     * @return Список {@link ServicesYaml.RequestResponse}.
     * @throws NullPointerException если {@code services} или {@code mapper} равны {@code null}.
     */
    private static List<ServicesYaml.RequestResponse> extractRequestResponse(
            List<ServicesYaml> services,
            Function<ServicesYaml.ValidatorJson, List<ServicesYaml.RequestResponse>> mapper
    ) {
        return services
                .stream()
                .map(ServicesYaml::getValidators)
                .map(ServicesYaml.Validator::getValidatorJson)
                .map(mapper)
                .filter(Predicate.not(CollectionUtils::isEmpty))
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Генерирует составной ID из конфигураций.
     * <p>
     * Пример: {@code "id1_id2_id3"}.
     * </p>
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @return Строка ID, разделённых "_" (пустая, если {@code services} пуст).
     * @throws NullPointerException если {@code services} равен {@code null}.
     */
    public static String id(List<ServicesYaml> services) {
        return services
                .stream()
                .map(ServicesYaml::getId)
                .collect(Collectors.joining("_"));
    }

    /**
     * Возвращает URL первого сервиса в списке.
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @return URL (пустая строка, если конфигурации отсутствуют).
     * @throws NullPointerException если {@code services} равен {@code null}.
     */
    public static String url(List<ServicesYaml> services) {
        return services
                .stream()
                .map(ServicesYaml::getUrl)
                .findFirst()
                .orElse("");
    }
}
