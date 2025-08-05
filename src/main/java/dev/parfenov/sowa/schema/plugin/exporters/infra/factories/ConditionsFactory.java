package dev.parfenov.sowa.schema.plugin.exporters.infra.factories;

import dev.parfenov.sowa.schema.plugin.exporters.infra.ServicesYaml;

/**
 * Фабрика для создания условий валидации.
 *
 * <p>Отвечает за создание объектов {@link ServicesYaml.Condition}, которые
 * определяют условия для выполнения действий валидации на основе
 * HTTP метода запроса и статуса ответа.</p>
 *
 * <p>Поддерживаемые типы условий:</p>
 * <ul>
 *   <li>Условия на основе HTTP метода запроса</li>
 *   <li>Условия на основе статуса ответа (для обработки ошибок)</li>
 * </ul>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-05
 */
public class ConditionsFactory {

    /**
     * Создает условие на основе HTTP метода запроса.
     *
     * @param val значение HTTP метода (например, "GET", "POST", "PUT", "DELETE")
     * @return условие для проверки метода запроса
     */
    public ServicesYaml.Condition createRequestMethodCondition(String val) {
        return createCondition("$request_method", "=", val);
    }

    /**
     * Создает условие на основе статуса ответа от upstream сервиса.
     *
     * @param val паттерн для проверки статуса (например, "^4\\d{2}$" для 4xx ошибок)
     * @return условие для проверки статуса ответа
     */
    public ServicesYaml.Condition createUpstreamStatusCondition(String val) {
        return createCondition("$upstream_status", "~", val);
    }

    /**
     * Создает условие с указанными параметрами.
     *
     * @param var      переменная для проверки (например, "$request_method", "$upstream_status")
     * @param operator оператор сравнения ("=" для точного соответствия, "~" для regex)
     * @param val      значение или паттерн для сравнения
     * @return настроенное условие
     */
    private ServicesYaml.Condition createCondition(String var, String operator, String val) {
        var condition = new ServicesYaml.Condition();
        condition.setVar(var);
        condition.setOperator(operator);
        condition.setVal(val);
        return condition;
    }

}
