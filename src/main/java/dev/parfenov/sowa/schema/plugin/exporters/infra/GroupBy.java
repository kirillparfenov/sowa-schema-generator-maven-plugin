/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.exporters.infra;

import dev.parfenov.sowa.schema.plugin.exporters.infra.factories.ServicesYamlFactory;

import java.util.List;
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
        var requestChains = Extract.requestsChain(services);
        var responseChains = Extract.responsesChains(services);

        return servicesFactory.createService(
                export,
                id,
                url,
                allowedQueries,
                groupByMessage(requestChains),
                groupByMessage(responseChains)
        );
    }

    /**
     * Собирает все actions воедино, группируя по {@link ServicesYaml.Chain#getMessage()}
     *
     * @param chainList не сгруппированный список
     * @return сгруппированный список
     */
    private List<ServicesYaml.Chain> groupByMessage(List<ServicesYaml.Chain> chainList) {
        return chainList.stream()
                .collect(Collectors.groupingBy(
                        ServicesYaml.Chain::getMessage,
                        Collectors.flatMapping(
                                chain -> chain.getActions().stream(),
                                Collectors.toList()
                        )
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    var chain = new ServicesYaml.Chain();
                    chain.setMessage(entry.getKey());
                    chain.setActions(entry.getValue());
                    return chain;
                })
                .collect(Collectors.toList());
    }
}
