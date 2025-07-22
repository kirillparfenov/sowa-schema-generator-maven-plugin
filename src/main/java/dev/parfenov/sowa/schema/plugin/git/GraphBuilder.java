/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.git;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.github.classgraph.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
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
 */
public class GraphBuilder {

    /**
     * Результат сканирования classpath с помощью ClassGraph
     */
    private final ScanResult scanResult;

    /**
     * Множество уже посещенных узлов графа для предотвращения циклических зависимостей
     */
    private Set<GraphModel> branchVisited;

    public GraphBuilder(ScanResult scanResult) {
        this.scanResult = scanResult;
        this.branchVisited = new HashSet<>();
    }

    /**
     * Строит графы зависимостей для всех методов указанного REST контроллера.
     * Анализирует типы параметров (запросы) и возвращаемые типы (ответы).
     *
     * @param method информация о методе REST контроллера (не может быть null)
     * @return объект Dependencies содержащий множества исходных файлов для запросов и ответов
     */
    public synchronized Dependencies buildGraphModels(MethodInfo method) {
        var dependencies = new Dependencies();
        processControllerMethod(method, dependencies);
        return dependencies;
    }

    /**
     * Обрабатывает один метод контроллера.
     *
     * @param method       метод для обработки
     * @param dependencies объект для сбора зависимостей
     */
    private void processControllerMethod(MethodInfo method, Dependencies dependencies) {
        buildResponse(method).ifPresent(graphModel ->
                appendSourceFiles(graphModel, dependencies.getResponse()));
        buildRequest(method).ifPresent(graphModel ->
                appendSourceFiles(graphModel, dependencies.getRequest()));
        cleanup();
    }

    /**
     * Строит граф зависимостей для типа возвращаемого значения метода.
     *
     * @param method информация о методе для анализа (не может быть null)
     * @return Optional содержащий граф зависимостей если тип найден, иначе пустой
     */
    private Optional<GraphModel> buildResponse(MethodInfo method) {
        var typeSignature = method.getTypeSignatureOrTypeDescriptor();
        if (typeSignature == null) {
            return Optional.empty();
        }

        var resultType = typeSignature.getResultType();
        if (resultType instanceof ClassRefTypeSignature ref) {
            return Optional.of(build(ref.getClassInfo(), ref.getTypeArguments()));
        }

        return Optional.empty();
    }

    /**
     * Строит граф зависимостей для типа параметра метода с аннотацией @RequestBody.
     *
     * @param method информация о методе для анализа (не может быть null)
     * @return Optional содержащий граф зависимостей если параметр найден, иначе пустой
     */
    private Optional<GraphModel> buildRequest(MethodInfo method) {
        for (var param : method.getParameterInfo()) {
            if (param != null && param.hasAnnotation(RequestBody.class)) {
                var typeSignature = param.getTypeSignatureOrTypeDescriptor();
                if (typeSignature instanceof ClassRefTypeSignature ref) {
                    return Optional.of(build(ref.getClassInfo(), ref.getTypeArguments()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Рекурсивно добавляет пути к исходным файлам из графа зависимостей в указанное множество.
     *
     * @param graphModel  узел графа зависимостей (не может быть null)
     * @param sourceFiles множество для добавления путей к исходным файлам (не может быть null)
     */
    private void appendSourceFiles(GraphModel graphModel, Set<String> sourceFiles) {
        var sourceFile = graphModel.getCurrent().getSourceFile();
        if (sourceFile != null) {
            sourceFiles.add(sourceFile);
        }

        var dependencies = graphModel.getDependencies();
        for (var dependency : dependencies) {
            appendSourceFiles(dependency, sourceFiles);
        }
    }

    /**
     * Инициализирует построение графа зависимостей для указанного класса.
     * Сбрасывает множество посещенных узлов перед началом построения.
     *
     * @param root     корневой класс для построения графа (не может быть null)
     * @param generics список аргументов генериков (может быть null)
     * @return корневой узел построенного графа зависимостей
     */
    private GraphModel build(ClassInfo root, List<TypeArgument> generics) {
        branchVisited = new HashSet<>();
        return buildGraph(root, generics);
    }

    /**
     * Строит узел графа зависимостей для указанного класса и его генериков.
     *
     * @param root     класс для которого строится узел (не может быть null)
     * @param generics список аргументов генериков (не может быть null)
     * @return узел графа с построенными зависимостями
     */
    private GraphModel buildGraph(ClassInfo root, List<TypeArgument> generics) {
        var node = new GraphModel(root, hash(generics));
        buildAll(node, generics);
        return node;
    }

    /**
     * Строит все типы зависимостей для указанного узла графа.
     * Включает суперклассы, интерфейсы, поля, подтипы и аргументы генериков.
     *
     * @param root      узел графа для которого строятся зависимости (не может быть null)
     * @param arguments список аргументов генериков для обработки (не может быть null)
     */
    private void buildAll(GraphModel root, List<TypeArgument> arguments) {
        if (visited(root)) {
            return;
        }

        appendClassHierarchy(root);
        appendGenerics(root, arguments);
    }

    /**
     * Добавляет зависимости от иерархии классов (суперклассы, интерфейсы, поля, подтипы).
     *
     * @param root узел графа для добавления зависимостей (не может быть null)
     */
    private void appendClassHierarchy(GraphModel root) {
        appendSuperClasses(root);
        appendInterfaces(root);
        appendFields(root);
        appendSubtypes(root);
    }

    /**
     * Добавляет зависимости от аргументов генериков.
     *
     * @param root      узел графа для добавления зависимостей (не может быть null)
     * @param arguments список аргументов генериков (не может быть null)
     */
    private void appendGenerics(GraphModel root, List<TypeArgument> arguments) {
        if (CollectionUtils.isEmpty(arguments)) {
            return;
        }

        for (var argument : arguments) {
            processGenericArgument(root, argument);
        }
    }

    /**
     * Обрабатывает один аргумент генерика.
     *
     * @param root     узел графа для добавления зависимости
     * @param argument аргумент генерика
     */
    private void processGenericArgument(GraphModel root, TypeArgument argument) {
        var typeSignature = argument.getTypeSignature();
        if (typeSignature instanceof ClassRefTypeSignature ref) {
            root.append(buildGraph(ref.getClassInfo(), ref.getTypeArguments()));
        }
    }

    /**
     * Добавляет зависимости от суперклассов в граф.
     * Обрабатывает как прямой суперкласс, так и всю иерархию суперклассов.
     *
     * @param root узел графа для добавления зависимостей от суперклассов (не может быть null)
     */
    private void appendSuperClasses(GraphModel root) {
        appendDirectSuperClass(root);
        appendInheritedSuperClasses(root);
    }

    /**
     * Добавляет прямой суперкласс.
     *
     * @param root узел графа
     */
    private void appendDirectSuperClass(GraphModel root) {
        var typeSignature = root.getCurrent().getTypeSignatureOrTypeDescriptor();
        if (typeSignature != null) {
            appendSuperClassRef(root, typeSignature.getSuperclassSignature());
        }
    }

    /**
     * Добавляет унаследованные суперклассы.
     *
     * @param root узел графа
     */
    private void appendInheritedSuperClasses(GraphModel root) {
        var superClasses = root.getCurrent().getSuperclasses();
        for (var superClass : superClasses) {
            if (superClass != null) {
                var typeSignature = superClass.getTypeSignatureOrTypeDescriptor();
                if (typeSignature != null) {
                    appendSuperClassRef(root, typeSignature.getSuperclassSignature());
                }
            }
        }
    }

    /**
     * Добавляет зависимость от суперкласса по его сигнатуре типа.
     *
     * @param root     узел графа для добавления зависимости (не может быть null)
     * @param superRef сигнатура типа суперкласса, может быть null
     */
    private void appendSuperClassRef(GraphModel root, ClassRefTypeSignature superRef) {
        if (superRef == null) {
            return;
        }

        var classInfo = superRef.getClassInfo();
        var typeArguments = superRef.getTypeArguments();

        if (classInfo != null) {
            appendSuperClass(root, classInfo, typeArguments != null ? typeArguments : List.of());
        }
    }

    /**
     * Добавляет зависимость от конкретного суперкласса.
     *
     * @param root      узел графа для добавления зависимости (не может быть null)
     * @param target    информация о суперклассе (не может быть null)
     * @param arguments аргументы генериков суперкласса (не может быть null)
     */
    private void appendSuperClass(GraphModel root, ClassInfo target, List<TypeArgument> arguments) {
        root.append(buildGraph(target, arguments));
    }

    /**
     * Добавляет зависимости от всех интерфейсов, реализуемых классом.
     *
     * @param root узел графа для добавления зависимостей от интерфейсов (не может быть null)
     */
    private void appendInterfaces(GraphModel root) {
        var interfaces = root.getCurrent().getInterfaces();
        for (var oneInterface : interfaces) {
            appendSuperClass(root, oneInterface, List.of());
        }
    }

    /**
     * Добавляет зависимости от типов всех полей класса.
     * Обрабатывает как прямые типы полей, так и их аргументы генериков.
     * Также проверяет поля на наличие подтипов через аннотации.
     *
     * @param root узел графа для добавления зависимостей от полей (не может быть null)
     */
    private void appendFields(GraphModel root) {
        var fields = root.getCurrent().getFieldInfo();
        for (var field : fields) {
            processField(root, field);
        }
    }

    /**
     * Обрабатывает одно поле класса.
     *
     * @param root  узел графа
     * @param field информация о поле
     */
    private void processField(GraphModel root, FieldInfo field) {
        var typeSignature = field.getTypeSignatureOrTypeDescriptor();
        if (typeSignature instanceof ClassRefTypeSignature ref) {
            buildField(root, ref);
            appendFieldGenerics(root, ref);
        }

        try {
            var loadedField = field.loadClassAndGetField();
            if (loadedField != null) {
                appendSubTypes(root, loadedField);
            }
        } catch (Exception e) {
            // Игнорируем ошибки загрузки поля
        }
    }

    /**
     * Добавляет зависимости от аргументов генериков поля.
     *
     * @param root узел графа
     * @param ref  сигнатура типа поля
     */
    private void appendFieldGenerics(GraphModel root, ClassRefTypeSignature ref) {
        var typeArguments = ref.getTypeArguments();
        if (typeArguments != null) {
            for (var arg : typeArguments) {
                if (arg.getTypeSignature() instanceof ClassRefTypeSignature argRef) {
                    buildField(root, argRef);
                }
            }
        }
    }

    /**
     * Строит зависимость от типа поля, если он принадлежит базовому пакету.
     *
     * @param root узел графа для добавления зависимости (не может быть null)
     * @param ref  сигнатура типа поля (не может быть null)
     */
    private void buildField(GraphModel root, ClassRefTypeSignature ref) {
        var classInfo = ref.getClassInfo();
        if (classInfo != null) {
            var typeArguments = ref.getTypeArguments();
            root.append(buildGraph(classInfo, typeArguments != null ? typeArguments : List.of()));
        }
    }

    /**
     * Добавляет зависимости от подтипов класса, определенных через аннотацию @JsonSubTypes.
     *
     * @param root узел графа для добавления зависимостей от подтипов (не может быть null)
     */
    private void appendSubtypes(GraphModel root) {
        var current = root.getCurrent();
        try {
            var loadedClass = current.loadClass();
            if (loadedClass != null) {
                var subtypes = searchSubTypes(loadedClass);
                for (var subtype : subtypes) {
                    root.append(buildGraph(subtype, List.of()));
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки загрузки класса
        }
    }

    /**
     * Добавляет зависимости от подтипов конкретного поля, определенных через аннотацию @JsonSubTypes.
     *
     * @param root  узел графа для добавления зависимостей (не может быть null)
     * @param field поле для анализа подтипов (не может быть null)
     */
    private void appendSubTypes(GraphModel root, Field field) {
        var subtypes = searchSubTypes(field);
        for (var subtype : subtypes) {
            root.append(buildGraph(subtype, List.of()));
        }
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
     * @param types массив подтипов
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
     *
     * @param subType подтип для проверки
     * @return true если подтип валидный
     */
    private boolean isValidSubType(JsonSubTypes.Type subType) {
        return subType.annotationType().isAssignableFrom(JsonSubTypes.Type.class);
    }

    /**
     * Получает информацию о классе из подтипа.
     *
     * @param subType подтип
     * @return информация о классе или null если не найдена
     */
    private ClassInfo getClassInfoFromSubType(JsonSubTypes.Type subType) {
        try {
            var valueClass = subType.value();
            if (valueClass != null) {
                return scanResult.getClassInfo(valueClass.getName());
            }
        } catch (Exception e) {
            // Игнорируем ошибки получения информации о классе
        }
        return null;
    }

    /**
     * Проверяет, был ли узел графа уже посещен, и добавляет его в множество посещенных.
     * Используется для предотвращения циклических зависимостей.
     *
     * @param root узел графа для проверки (не может быть null)
     * @return true если узел уже был посещен, false если это первое посещение
     */
    private boolean visited(GraphModel root) {
        var contains = branchVisited.contains(root);
        branchVisited.add(root);
        return contains;
    }

    /**
     * Вычисляет хеш для списка аргументов генериков.
     * Используется для различения узлов графа с одним классом но разными генериками.
     *
     * @param generics список аргументов генериков (может быть null)
     * @return хеш-код списка генериков
     */
    private int hash(List<TypeArgument> generics) {
        return generics.hashCode();
    }

    /**
     * Очищает множество посещенных узлов после завершения построения графа.
     * Необходимо вызывать после каждого цикла построения графа.
     */
    private void cleanup() {
        this.branchVisited.clear();
    }
}
