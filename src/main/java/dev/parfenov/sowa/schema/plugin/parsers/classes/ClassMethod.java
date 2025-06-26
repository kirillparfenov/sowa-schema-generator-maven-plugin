package dev.parfenov.sowa.schema.plugin.parsers.classes;

import com.fasterxml.classmate.ResolvedType;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Метод из класса с аннотацией {@link RestController}
 */
public record ClassMethod(
        /// Имя метода для SOWA
        String restControllerMethodName,

        /// Тело запроса
        ResolvedType request,

        /// Тело ответа
        ResolvedType response,

        /// HTTP-метод
        HttpMethod httpMethod,

        /// Адрес эндпоинта
        String endpointUrl

        //todo добавить перечисление всех параметров метода (для обработки @PathVariable)
) {
}
