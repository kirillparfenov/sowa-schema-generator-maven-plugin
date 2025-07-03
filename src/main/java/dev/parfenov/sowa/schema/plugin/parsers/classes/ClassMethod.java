package dev.parfenov.sowa.schema.plugin.parsers.classes;

import com.fasterxml.classmate.ResolvedType;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Метод из класса с аннотацией {@link RestController}
 */
public record ClassMethod(
        /// Имя метода для SOWA
        String endpointName,

        /// Тело запроса
        ResolvedType request,

        /// Тело ответа
        ResolvedType response,

        /// HTTP-метод
        HttpMethod httpMethod,

        /// Адрес эндпоинта
        String endpointUrl,

        /// Переменные пути запроса
        List<PathVariableParam> pathVariableParams
) {}
