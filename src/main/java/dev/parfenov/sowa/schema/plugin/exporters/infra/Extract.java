/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporters.infra;

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
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
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
     * @return Список {@link ServicesYaml.Chain} (пустой, если валидаторы отсутствуют).
     * @throws NullPointerException если {@code services} равен {@code null}.
     */
    public static List<ServicesYaml.Chain> requestsChain(List<ServicesYaml> services) {
        return extractRequestResponse(services, ServicesYaml.Chains::getRequestChains);
    }

    /**
     * Извлекает валидаторы ответов из конфигураций.
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @return Список {@link ServicesYaml.Chain} (пустой, если валидаторы отсутствуют).
     * @throws NullPointerException если {@code services} равен {@code null}.
     */
    public static List<ServicesYaml.Chain> responsesChains(List<ServicesYaml> services) {
        return extractRequestResponse(services, ServicesYaml.Chains::getResponseChains);
    }

    /**
     * Вспомогательный метод для извлечения валидаторов (запросов или ответов).
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @param mapper   Функция-селектор ({@code getRequest} или {@code getResponse}).
     * @return Список {@link ServicesYaml.Chain}.
     * @throws NullPointerException если {@code services} или {@code mapper} равны {@code null}.
     */
    private static List<ServicesYaml.Chain> extractRequestResponse(
            List<ServicesYaml> services,
            Function<ServicesYaml.Chains, List<ServicesYaml.Chain>> mapper
    ) {
        return services
                .stream()
                .map(ServicesYaml::getChains)
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

    /**
     * Возвращает {@code true}, если хотя бы один элемент требует экспорта
     *
     * @param services Список конфигураций {@link ServicesYaml}.
     * @return {@code true}, если хотя бы один элемент требует экспорта
     */
    public static boolean export(List<ServicesYaml> services) {
        return services.stream().anyMatch(ServicesYaml::isExport);
    }
}
