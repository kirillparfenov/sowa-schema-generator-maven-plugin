package dev.parfenov.sowa.schema.plugin.exporters.infra.factories;

import dev.parfenov.sowa.schema.plugin.exporters.infra.Actions;
import dev.parfenov.sowa.schema.plugin.exporters.infra.InfraConfig;
import dev.parfenov.sowa.schema.plugin.exporters.infra.ServicesYaml;
import dev.parfenov.sowa.schema.plugin.parsers.TypesParser;
import dev.parfenov.sowa.schema.plugin.parsers.dto.ClassModel;
import dev.parfenov.sowa.schema.plugin.parsers.dto.MethodModel;

import java.util.List;

/**
 * Фабрика для создания действий (actions) валидации запросов и ответов.
 *
 * <p>Отвечает за создание объектов {@link ServicesYaml.Action} с соответствующими
 * типами действий, условиями и параметрами для валидации HTTP запросов и ответов,
 * а также обработки ошибок 4xx.</p>
 *
 * <p>Поддерживаемые типы действий:</p>
 * <ul>
 *   <li>{@code JSON_VALIDATION} - для валидации JSON данных</li>
 *   <li>{@code CHECK_DATA_SIZE} - для проверки размера данных</li>
 * </ul>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-05
 */
public class ActionsFactory {
    private static final String ERROR_4XX_PATTERN = "^4\\d{2}$";
    private final ParamFactory paramFactory;
    private final ConditionsFactory conditionsFactory = new ConditionsFactory();

    /**
     * Создает новый экземпляр фабрики действий.
     *
     * @param infraConfig конфигурация инфраструктуры для создания параметров
     */
    public ActionsFactory(InfraConfig infraConfig) {
        this.paramFactory = new ParamFactory(infraConfig);
    }

    /**
     * Создает действие для валидации HTTP запроса.
     *
     * <p>Выбирает тип действия на основе наличия тела запроса и HTTP метода:
     * <ul>
     *   <li>Для GET запросов или запросов без тела - {@code CHECK_DATA_SIZE}</li>
     *   <li>Для остальных запросов - {@code JSON_VALIDATION}</li>
     * </ul></p>
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return действие для валидации запроса
     */
    public ServicesYaml.Action createRequestAction(ClassModel classModel, MethodModel method) {
        var actions = method.getRequest().getType() == null || TypesParser.isVoid(method.getRequest().getType())
                ? Actions.CHECK_DATA_SIZE
                : Actions.JSON_VALIDATION;

        var param = paramFactory.createRequestParam(classModel, method, actions);
        var condition = conditionsFactory.createRequestMethodCondition(method.httpMethodName());
        return createAction(
                actions,
                List.of(condition),
                param
        );
    }

    /**
     * Создает действие для валидации HTTP ответа.
     *
     * <p>Выбирает тип действия на основе наличия тела ответа:
     * <ul>
     *   <li>Для ответов без тела - {@code CHECK_DATA_SIZE}</li>
     *   <li>Для ответов с телом - {@code JSON_VALIDATION}</li>
     * </ul></p>
     *
     * @param classModel модель класса контроллера
     * @param method     модель метода endpoint'а
     * @return действие для валидации ответа
     */
    public ServicesYaml.Action createResponseAction(ClassModel classModel, MethodModel method) {
        var actions = method.getResponse().getType() == null || TypesParser.isVoid(method.getResponse().getType())
                ? Actions.CHECK_DATA_SIZE
                : Actions.JSON_VALIDATION;

        var param = paramFactory.createResponseParam(classModel, method, actions);
        var condition = conditionsFactory.createRequestMethodCondition(method.httpMethodName());
        return createAction(
                actions,
                List.of(condition),
                param
        );
    }

    /**
     * Создает действие для валидации ошибок 4xx.
     *
     * <p>Создает действие JSON валидации с условием проверки статуса ответа
     * на соответствие паттерну 4xx (клиентские ошибки).</p>
     *
     * @return действие для валидации ошибок 4xx
     */
    public ServicesYaml.Action create4xxAction() {
        var actions = Actions.JSON_VALIDATION;
        var param = paramFactory.create4xxParam(actions);
        var condition = conditionsFactory.createUpstreamStatusCondition(ERROR_4XX_PATTERN);
        return createAction(
                actions,
                List.of(condition),
                param
        );
    }

    /**
     * Создает объект действия с указанными параметрами.
     *
     * @param actions    тип действия
     * @param conditions список условий для выполнения действия
     * @param param      параметры действия
     * @return настроенный объект действия
     */
    private ServicesYaml.Action createAction(Actions actions, List<ServicesYaml.Condition> conditions, ServicesYaml.Param param) {
        var action = new ServicesYaml.Action();
        action.setAction(actions.getAction());
        action.setConditions(conditions);
        action.setParams(param);
        return action;
    }
}
