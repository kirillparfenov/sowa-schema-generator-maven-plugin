/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporters.infra;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Объединяет конфигурации по полям:
 * <ul>
 *     <li>URL</li>
 * </ul>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class GroupBy {

    private final ServicesYamlFactory servicesFactory;

    public GroupBy(ServicesYamlFactory servicesFactory) {
        this.servicesFactory = servicesFactory;
    }

    /**
     * Объединяет конфигурации в блоках:
     * <ul>
     *     <li>allowed_queries</li>
     *     <li>request</li>
     *     <li>response</li>
     * </ul>
     *
     * @param services готовые конфигурации
     * @return конфигурации, объединенные по полю URL
     */
    public List<ServicesYaml> url(List<ServicesYaml> services) {
        var map = services
                .stream()
                .collect(Collectors.toMap(
                        ServicesYaml::getUrl,
                        List::of,
                        (old, current) -> Stream.concat(old.stream(), current.stream()).toList()
                ));

        return map.values()
                .stream()
                .map(this::groupByUrl)
                .collect(Collectors.toList());
    }

    /**
     * Группирует поля разных сервисов в одну конфигурацию:
     * <ul>
     *     <li>allowed_queries</li>
     *     <li>request</li>
     *     <li>response</li>
     * </ul>
     *
     * @param services конфигурации
     * @return конфигурация со сгруппированными полями
     */
    private ServicesYaml groupByUrl(List<ServicesYaml> services) {
        var export = Extract.export(services);
        var id = Extract.id(services);
        var url = Extract.url(services);
        var allowedQueries = Extract.allowedQueries(services);
        var request = Extract.requests(services);
        var response = Extract.responses(services);
        var validatorJson = servicesFactory.createValidatorJson(request, response);

        return servicesFactory.createService(
                export,
                id,
                url,
                allowedQueries,
                servicesFactory.createValidator(validatorJson)
        );
    }
}
