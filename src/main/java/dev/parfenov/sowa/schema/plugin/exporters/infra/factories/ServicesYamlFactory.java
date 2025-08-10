package dev.parfenov.sowa.schema.plugin.exporters.infra.factories;

import dev.parfenov.sowa.schema.plugin.config.InfraConfig;
import dev.parfenov.sowa.schema.plugin.exporters.infra.ServicesYaml;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;

import java.util.List;

/**
 * Фабрика для создания объектов ServicesYaml и связанных с ними компонентов.
 * Инкапсулирует логику создания конфигурационных объектов для services.yml файла.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class ServicesYamlFactory {

    private final AllowedQueryFactory aQueryFactory = new AllowedQueryFactory();
    private final ChainsFactory chainsFactory;

    public ServicesYamlFactory(InfraConfig infraConfig) {
        this.chainsFactory = new ChainsFactory(infraConfig);
    }

    public ServicesYaml createService(boolean export, String id, String url,
                                      List<ServicesYaml.AllowedQuery> allowedQueries,
                                      List<ServicesYaml.Chain> requestChains, List<ServicesYaml.Chain> responseChains) {
        var serviceYaml = new ServicesYaml();
        serviceYaml.setExport(export);
        serviceYaml.setId(id);
        serviceYaml.setName(id);
        serviceYaml.setUrl(url);
        serviceYaml.setAllowedQueries(allowedQueries);
        serviceYaml.setChains(chainsFactory.createChains(requestChains, responseChains));
        return serviceYaml;
    }

    public ServicesYaml createService(boolean export, String id, String url,
                                      ClassModel classModel, MethodModel method) {
        var serviceYaml = new ServicesYaml();
        serviceYaml.setExport(export);
        serviceYaml.setId(id);
        serviceYaml.setName(id);
        serviceYaml.setUrl(url);
        serviceYaml.setAllowedQueries(aQueryFactory.createAllowedQueries(method.httpMethodName().toLowerCase()));
        serviceYaml.setChains(chainsFactory.createChains(classModel, method));
        return serviceYaml;
    }

    public void append4xxResponse(List<ServicesYaml> servicesYaml) {
        chainsFactory.append4xxAction(servicesYaml);
    }
}