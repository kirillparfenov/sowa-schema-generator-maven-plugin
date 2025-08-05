package dev.parfenov.sowa.schema.plugin.exporters.infra.factories;

import dev.parfenov.sowa.schema.plugin.exporters.infra.InfraConfig;
import dev.parfenov.sowa.schema.plugin.exporters.infra.ServicesYaml;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Фабрика для создания цепочек обработки запросов и ответов.
 *
 * <p>Отвечает за создание объектов {@link ServicesYaml.Chains} и {@link ServicesYaml.Chain},
 * которые определяют последовательность действий для валидации HTTP запросов и ответов.
 * Автоматически добавляет обработку ошибок 4xx к цепочкам ответов.</p>
 *
 * <p>Поддерживаемые типы цепочек:</p>
 * <ul>
 *   <li>Request chains - для валидации входящих запросов</li>
 *   <li>Response chains - для валидации исходящих ответов (включая обработку 4xx ошибок)</li>
 * </ul>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-05
 */
public class ChainsFactory {

    private static final String REQUEST_BODY = "$clj_request_body";
    private static final String RESPONSE_BODY = "$clj_response_body";
    private final ActionsFactory actionsFactory;

    /**
     * Создает новый экземпляр фабрики цепочек.
     *
     * @param infraConfig конфигурация инфраструктуры для создания действий
     */
    public ChainsFactory(InfraConfig infraConfig) {
        this.actionsFactory = new ActionsFactory(infraConfig);
    }

    /**
     * Создает объект цепочек для указанного метода.
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return объект с цепочками запросов и ответов
     */
    public ServicesYaml.Chains createChains(ClassModel classModel, MethodModel method) {
        var requests = createRequestChain(classModel, method);
        var responses = createResponseChain(classModel, method);
        return createChains(requests, responses);
    }

    /**
     * Создает объект цепочек из готовых списков цепочек запросов и ответов.
     *
     * @param requestChains  список цепочек для обработки запросов
     * @param responseChains список цепочек для обработки ответов
     * @return объект с указанными цепочками
     */
    public ServicesYaml.Chains createChains(List<ServicesYaml.Chain> requestChains, List<ServicesYaml.Chain> responseChains) {
        var chains = new ServicesYaml.Chains();
        chains.setRequestChains(requestChains);
        chains.setResponseChains(responseChains);
        return chains;
    }

    /**
     * Создает список цепочек для обработки запросов.
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return список цепочек запросов (может быть пустым)
     */
    public List<ServicesYaml.Chain> createRequestChain(ClassModel classModel, MethodModel method) {
        return Optional
                .ofNullable(create2xxRequest(classModel, method))
                .map(List::of)
                .orElseGet(List::of);
    }

    /**
     * Создает список цепочек для обработки ответов.
     * Автоматически добавляет обработку ошибок 4xx к существующим цепочкам.
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return список цепочек ответов (может быть пустым)
     */
    private List<ServicesYaml.Chain> createResponseChain(ClassModel classModel, MethodModel method) {
        return Optional
                .ofNullable(create2xxResponse(classModel, method))
                .map(this::append4xxAction)
                .map(List::of)
                .orElseGet(List::of);
    }

    /**
     * Создает цепочку для успешных запросов (2xx).
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return цепочка для обработки запросов
     */
    public ServicesYaml.Chain create2xxRequest(ClassModel classModel, MethodModel method) {
        return createChain(REQUEST_BODY, List.of(actionsFactory.createRequestAction(classModel, method)));
    }

    /**
     * Создает цепочку для успешных ответов (2xx).
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return цепочка для обработки ответов
     */
    private ServicesYaml.Chain create2xxResponse(ClassModel classModel, MethodModel method) {
        return createChain(RESPONSE_BODY, List.of(actionsFactory.createResponseAction(classModel, method)));
    }

    /**
     * Создает цепочку с указанным сообщением и действиями.
     *
     * @param message идентификатор сообщения для цепочки
     * @param actions список действий в цепочке
     * @return настроенную цепочку
     */
    public ServicesYaml.Chain createChain(String message, List<ServicesYaml.Action> actions) {
        var chain = new ServicesYaml.Chain();
        chain.setMessage(message);
        chain.setActions(actions);
        return chain;
    }

    /**
     * Добавляет действие обработки ошибок 4xx к существующей цепочке.
     *
     * @param chain исходная цепочка
     * @return цепочка с добавленным действием обработки 4xx ошибок
     */
    private ServicesYaml.Chain append4xxAction(ServicesYaml.Chain chain) {
        var chainActions = Stream
                .of(chain.getActions(), List.of(actionsFactory.create4xxAction()))
                .flatMap(Collection::stream)
                .toList();
        chain.setActions(chainActions);
        return chain;
    }
}
