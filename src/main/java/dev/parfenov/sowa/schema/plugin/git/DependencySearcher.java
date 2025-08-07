package dev.parfenov.sowa.schema.plugin.git;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import dev.parfenov.sowa.schema.plugin.parsers.ClassParser;
import io.github.classgraph.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.AnnotatedElement;
import java.util.*;

/**
 * Сервис для построения графа зависимостей классов на основе анализа REST контроллеров.
 * Анализирует методы контроллеров, строит граф зависимостей для типов запросов и ответов,
 * включая все связанные классы, интерфейсы, суперклассы и подтипы.
 *
 * <p>Основные возможности:</p>
 * <ul>
 *   <li>Анализ типов параметров методов контроллеров (с аннотацией @RequestBody)</li>
 *   <li>Анализ типов возвращаемых значений методов</li>
 *   <li>Построение полного графа зависимостей включая поля, суперклассы, интерфейсы</li>
 *   <li>Обработка генериков и их аргументов</li>
 *   <li>Поддержка полиморфизма через @JsonSubTypes</li>
 *   <li>Предотвращение циклических зависимостей</li>
 * </ul>
 *
 * <p>Класс является потокобезопасным.</p>
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-03
 */
public class DependencySearcher {

    /**
     * Результат сканирования classpath с помощью ClassGraph
     */
    private final ScanResult scanResult;

    private final ClassParser classParser;

    /**
     * Создает новый экземпляр DependencySearcher.
     *
     * @param scanResult результат сканирования classpath (не может быть null)
     * @throws IllegalArgumentException если scanResult равен null
     */
    public DependencySearcher(ScanResult scanResult, ClassParser classParser) {
        this.scanResult = scanResult;
        this.classParser = classParser;
    }

    /**
     * Строит графы зависимостей для всех методов указанного REST контроллера.
     * Анализирует типы параметров (запросы) и возвращаемые типы (ответы).
     *
     * @param method информация о методе REST контроллера (не может быть null)
     * @return объект Dependencies содержащий множества исходных файлов для запросов и ответов
     * @throws IllegalArgumentException если method равен null
     */
    public Dependencies searchDependencies(MethodInfo method) {
        var dependencies = new Dependencies();
        processControllerMethod(method, dependencies);
        return dependencies;
    }

    /**
     * Обрабатывает один метод контроллера, извлекая зависимости из типов запроса и ответа.
     * Выполняет поиск зависимостей для возвращаемого типа метода и параметров с аннотацией @RequestBody.
     *
     * @param method       метод для обработки (не может быть null)
     * @param dependencies объект для сбора зависимостей (не может быть null)
     */
    private void processControllerMethod(MethodInfo method, Dependencies dependencies) {
        getMethodResponse(method).ifPresent(response ->
                dfs(response, dependencies.getResponse())
        );

        getMethodRequest(method).ifPresent(request ->
                dfs(request, dependencies.getRequest())
        );
    }

    /**
     * Извлекает тип возвращаемого значения метода.
     *
     * @param method метод для анализа (не может быть null)
     * @return Optional с типом возвращаемого значения, если это ClassRefTypeSignature, иначе пустой Optional
     */
    private Optional<ClassRefTypeSignature> getMethodResponse(MethodInfo method) {
        var resultType = method.getTypeSignatureOrTypeDescriptor().getResultType();
        return resultType instanceof ClassRefTypeSignature ref
                ? Optional.of(ref)
                : Optional.empty();
    }

    /**
     * Извлекает тип параметра метода с аннотацией @RequestBody.
     *
     * @param method метод для анализа (не может быть null)
     * @return Optional с типом параметра запроса, если найден, иначе пустой Optional
     */
    private Optional<ClassRefTypeSignature> getMethodRequest(MethodInfo method) {
        if (method.getParameterInfo() == null) {
            return Optional.empty();
        }
        return Arrays
                .stream(method.getParameterInfo())
                .filter(param -> param.hasAnnotation(RequestBody.class))
                .map(MethodParameterInfo::getTypeSignatureOrTypeDescriptor)
                .map(param -> param instanceof ClassRefTypeSignature ref ? ref : null)
                .filter(Objects::nonNull)
                .findFirst();
    }

    /**
     * Выполняет поиск зависимостей в глубину для типа класса.
     * Извлекает все связанные классы из сигнатуры типа и запускает поиск для каждого.
     *
     * @param ref          сигнатура типа класса для анализа (может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    private void dfs(ClassRefTypeSignature ref, Set<String> dependencies) {
        Optional.ofNullable(ref)
                .map(this::extract)
                .orElseGet(List::of)
                .forEach(classInfo -> dfs(classInfo, dependencies));
    }

    /**
     * Выполняет поиск зависимостей в глубину для информации о классе.
     * Предотвращает циклические зависимости, проверяя, не был ли файл уже обработан.
     *
     * @param classInfo    информация о классе для анализа (не может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    private void dfs(ClassInfo classInfo, Set<String> dependencies) {
        if (dependencies.contains(classInfo.getSourceFile())) {
            return;
        }
        appendDependency(classInfo, dependencies);
        collectClassHierarchy(classInfo, dependencies);
    }

    /**
     * Собирает всю иерархию класса включая подтипы, поля, суперклассы и интерфейсы.
     *
     * @param classInfo    информация о классе (не может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    private void collectClassHierarchy(ClassInfo classInfo, Set<String> dependencies) {
        collectClassSubtypes(classInfo, dependencies);
        collectFields(classInfo, dependencies);
        collectSuperClass(classInfo, dependencies);
        collectInterfaces(classInfo, dependencies);
    }

    /**
     * Собирает зависимости из полей класса.
     * Анализирует типы полей и их подтипы, определенные через @JsonSubTypes.
     *
     * @param classInfo    информация о классе (не может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    void collectFields(ClassInfo classInfo, Set<String> dependencies) {
        for (var field : classInfo.getFieldInfo()) {
            if (field.getTypeSignatureOrTypeDescriptor() instanceof ClassRefTypeSignature ref) {
                dfs(ref, dependencies);
            }
            collectFieldSubtypes(field, dependencies);
        }
    }

    /**
     * Собирает подтипы класса, определенные через аннотацию @JsonSubTypes.
     *
     * @param classInfo    информация о классе (не может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    void collectClassSubtypes(ClassInfo classInfo, Set<String> dependencies) {
        for (var classSubType : searchSubTypes(classInfo)) {
            dfs(classSubType, dependencies);
        }
    }

    /**
     * Собирает подтипы поля, определенные через аннотацию @JsonSubTypes.
     *
     * @param field        информация о поле (не может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    void collectFieldSubtypes(FieldInfo field, Set<String> dependencies) {
        try {
            for (var methodSubtype : searchSubTypes(field.loadClassAndGetField())) {
                dfs(methodSubtype, dependencies);
            }
        } catch (Exception ignore) {}
    }

    /**
     * Собирает зависимости из суперклассов.
     *
     * @param classInfo    информация о классе (не может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    void collectSuperClass(ClassInfo classInfo, Set<String> dependencies) {
        var superRef = classInfo.getTypeSignatureOrTypeDescriptor().getSuperclassSignature();
        if (superRef != null) {
            dfs(superRef, dependencies);
        }

        for (var superClass : classInfo.getSuperclasses()) {
            collectSuperClass(superClass, dependencies);
        }
    }

    /**
     * Собирает зависимости из интерфейсов, реализуемых классом.
     *
     * @param classInfo    информация о классе (не может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    void collectInterfaces(ClassInfo classInfo, Set<String> dependencies) {
        for (var interface_ : classInfo.getInterfaces()) {
            dfs(interface_, dependencies);
        }
    }

    /**
     * Извлекает информацию о всех классах из сигнатуры типа, включая аргументы типов.
     * Рекурсивно обрабатывает все вложенные типы и дженерики.
     *
     * @param ref сигнатура типа класса (не может быть null)
     * @return список информации о всех связанных классах
     */
    List<ClassInfo> extract(ClassRefTypeSignature ref) {
        var allTypesInfo = new ArrayList<ClassInfo>();
        recursiveCollectCLassInfo(ref.getClassInfo(), ref.getTypeArguments(), allTypesInfo);
        return allTypesInfo;
    }

    /**
     * Рекурсивно собирает информацию о классах из корневого класса и его аргументов типов.
     *
     * @param root         корневой класс (не может быть null)
     * @param arguments    аргументы типов (не может быть null)
     * @param allTypesInfo список для сбора информации о классах (не может быть null)
     */
    void recursiveCollectCLassInfo(ClassInfo root, List<TypeArgument> arguments, List<ClassInfo> allTypesInfo) {
        if (classParser.isProjectPackage(root)) {
            allTypesInfo.add(root);
        }
        for (var typeArgument : arguments) {
            if (typeArgument.getTypeSignature() instanceof ClassRefTypeSignature ref) {
                recursiveCollectCLassInfo(ref.getClassInfo(), ref.getTypeArguments(), allTypesInfo);
            }
        }
    }

    /**
     * Добавляет путь к исходному файлу класса в множество зависимостей.
     *
     * @param classInfo    информация о классе (может быть null)
     * @param dependencies множество для сбора зависимостей (не может быть null)
     */
    void appendDependency(ClassInfo classInfo, Set<String> dependencies) {
        Optional.ofNullable(classInfo)
                .map(ClassInfo::getSourceFile)
                .ifPresent(dependencies::add);
    }

    /**
     * Ищет подтипы для класса через аннотацию @JsonSubTypes.
     *
     * @param onClass класс для поиска подтипов (не может быть null)
     * @return список информации о найденных подтипах
     */
    private List<ClassInfo> searchSubTypes(ClassInfo onClass) {
        return Optional.ofNullable(onClass.getAnnotationInfo(JsonSubTypes.class))
                .map(AnnotationInfo::loadClassAndInstantiate)
                .map(JsonSubTypes.class::cast)
                .map(JsonSubTypes::value)
                .map(this::extractClassInfoFromSubTypes)
                .orElseGet(List::of);
    }

    /**
     * Ищет подтипы для аннотированного элемента через аннотацию @JsonSubTypes.
     *
     * @param annotatedElement элемент (класс или поле) для поиска подтипов (не может быть null)
     * @return список информации о найденных подтипах
     */
    private List<ClassInfo> searchSubTypes(AnnotatedElement annotatedElement) {
        var subTypes = annotatedElement.getAnnotation(JsonSubTypes.class);
        if (subTypes == null) {
            return List.of();
        }

        return extractClassInfoFromSubTypes(subTypes.value());
    }

    /**
     * Извлекает информацию о классах из массива подтипов.
     *
     * @param types массив подтипов (может быть null)
     * @return список информации о классах
     */
    private List<ClassInfo> extractClassInfoFromSubTypes(JsonSubTypes.Type[] types) {
        if (types == null) {
            return List.of();
        }

        return Arrays.stream(types)
                .filter(Objects::nonNull)
                .filter(this::isValidSubType)
                .map(this::getClassInfoFromSubType)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Проверяет, является ли подтип валидным.
     * Проверяет, что класс подтипа не является Object.class (что указывает на отсутствие конкретного типа).
     *
     * @param subType подтип для проверки (не может быть null)
     * @return true если подтип валидный, false если это Object.class или другой невалидный тип
     */
    private boolean isValidSubType(JsonSubTypes.Type subType) {
        try {
            return subType.value() != null && !Object.class.equals(subType.value());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Получает информацию о классе из подтипа.
     *
     * @param subType подтип (не может быть null)
     * @return информация о классе или null если не найдена
     */
    private ClassInfo getClassInfoFromSubType(JsonSubTypes.Type subType) {
        try {
            var valueClass = subType.value();
            if (valueClass != null) {
                return scanResult.getClassInfo(valueClass.getName());
            }
        } catch (Exception ignore) {
            return null;
        }
        return null;
    }
}
