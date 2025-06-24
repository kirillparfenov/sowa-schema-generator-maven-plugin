package dev.parfenov.sowa.schema.plugin.classparser;

import com.fasterxml.classmate.ResolvedType;
import dev.parfenov.sowa.schema.plugin.generator.RestControllerMethod;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Парсинг классов
 */
public interface ClassParser {

    /**
     * @return массив найденных методов в классах с аннотацией {@link RestController}
     * */
    List<RestControllerMethod> getAllMethods();
}
