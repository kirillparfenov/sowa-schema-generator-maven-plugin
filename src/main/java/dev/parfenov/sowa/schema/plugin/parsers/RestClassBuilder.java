package dev.parfenov.sowa.schema.plugin.parsers;

import dev.parfenov.sowa.schema.plugin.parsers.dto.RestClass;
import dev.parfenov.sowa.schema.plugin.parsers.dto.RestMethod;
import io.github.classgraph.ClassInfo;
import org.springframework.util.DigestUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder для создания объектов RestClass.
 * <p>
 * Предоставляет удобный способ пошагового конструирования объектов
 * REST классов с обработкой основного класса и интерфейсов.
 */
public class RestClassBuilder {

    private final RestClass restClass;
    private final Map<String, RestMethod> methodMap;
    private final EndpointPathParser endpointPathParser;
    private final TypesParser typesParser;

    /**
     * Конструктор builder'а.
     *
     * @param endpointPathParser парсер путей эндпоинтов
     * @param typesParser        парсер типов
     */
    public RestClassBuilder(EndpointPathParser endpointPathParser, TypesParser typesParser) {
        this.restClass = new RestClass();
        this.methodMap = new HashMap<>();
        this.endpointPathParser = endpointPathParser;
        this.typesParser = typesParser;
    }

    /**
     * Устанавливает имя класса.
     *
     * @param name имя класса
     * @return builder для цепочки вызовов
     */
    public RestClassBuilder withName(String name) {
        restClass.setName(name);
        return this;
    }

    /**
     * Обрабатывает основной класс контроллера.
     *
     * @param classInfo информация о классе
     * @return builder для цепочки вызовов
     */
    public RestClassBuilder withMainClass(ClassInfo classInfo) {
        processClass(classInfo);
        return this;
    }

    /**
     * Добавляет интерфейс к обработке.
     *
     * @param interfaceInfo информация об интерфейсе
     * @return builder для цепочки вызовов
     */
    public RestClassBuilder withInterface(ClassInfo interfaceInfo) {
        processClass(interfaceInfo);
        return this;
    }

    /**
     * Возвращает построенный объект REST класса.
     *
     * @return готовый объект RestClass
     */
    public RestClass build() {
        restClass.setMethods(new ArrayList<>(methodMap.values()));
        return restClass;
    }

    /**
     * Обрабатывает класс или интерфейс, извлекая методы и устанавливая путь.
     *
     * @param classInfo информация о классе/интерфейсе
     */
    private void processClass(ClassInfo classInfo) {
        setEndpointPathIfEmpty(classInfo);
        extractClassMethods(classInfo);
    }

    /**
     * Устанавливает путь эндпоинта, если он не задан.
     *
     * @param classInfo информация о классе
     */
    private void setEndpointPathIfEmpty(ClassInfo classInfo) {
        if (!StringUtils.hasText(restClass.getEndpointPath())) {
            var endpointPath = endpointPathParser.pathOnClass(classInfo);
            restClass.setEndpointPath(endpointPath);
        }
    }

    /**
     * Извлекает методы класса с аннотациями маппинга.
     *
     * @param classInfo информация о классе
     */
    private void extractClassMethods(ClassInfo classInfo) {
        var resolvedClass = typesParser.resolveErasedType(classInfo.loadClass());
        var classMembers = typesParser.resolveTypeMembers(resolvedClass);

        for (var method : classMembers.getMemberMethods()) {
            var requestMapping = AnnotationParser.directOnMethodOrAnnotations(RequestMapping.class, method.getRawMember());
            if (requestMapping != null) {
                var mapKey = createUniqueMethodName(method.getRawMember());
                var existingMethod = methodMap.getOrDefault(mapKey, new RestMethod());
                var restMethod = buildRestMethod(method.getRawMember(), existingMethod);
                methodMap.put(mapKey, restMethod);
            }
        }
    }

    /**
     * Создает уникальное имя метода с MD5 хешем.
     *
     * @param method метод для обработки
     * @return уникальное имя метода
     */
    private String createUniqueMethodName(Method method) {
        var methodName = method.getName();
        var methodBytes = SerializationUtils.serialize(method.toString());
        var md5 = DigestUtils.md5DigestAsHex(methodBytes != null ? methodBytes : methodName.getBytes());
        return methodName + "_" + md5;
    }

    /**
     * Строит объект REST метода из Java метода.
     *
     * @param method         Java метод
     * @param existingMethod существующий объект метода
     * @return построенный REST метод
     */
    private RestMethod buildRestMethod(Method method, RestMethod existingMethod) {
        return new RestMethodBuilder(method, existingMethod, endpointPathParser)
                .withName()
                .withPathVariables()
                .withRequest()
                .withResponse()
                .withEndpointPath()
                .withHttpMethod()
                .build();
    }
} 