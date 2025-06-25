package dev.parfenov.sowa.schema.plugin.classparser;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Парсинг классов
 */
public interface ClassParser {

    /**
     * @return массив найденных методов в классах с аннотацией {@link RestController}
     * */
    List<ClassMethod> getAllRestControllersMethods();
}
