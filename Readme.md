# SOWA Schema Generator Maven Plugin

[![Java CI with Maven](https://github.com/kirillparfenov/sowa-schema-generator-maven-plugin/actions/workflows/maven.yml/badge.svg)](https://github.com/kirillparfenov/sowa-schema-generator-maven-plugin/actions/workflows/maven.yml)

Maven плагин для автоматической генерации JSON схем из Spring REST контроллеров для системы SOWA.

## Описание

Плагин анализирует ваши Spring REST контроллеры и автоматически генерирует:
- JSON схемы для валидации запросов и ответов
- Инфраструктурные файлы для интеграции с SOWA
- Поддержка Jakarta Validation аннотаций
- Интеграция с Git для обработки только измененных файлов

## Требования

- Java 17+
- Maven 3.6+
- Spring Framework (для REST контроллеров)

## Установка

Добавьте плагин в ваш `pom.xml`:

```xml
<plugin>
    <groupId>dev.parfenov</groupId>
    <artifactId>sowa-schema-generator-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <sowaProfileName>super_profile_name</sowaProfileName>
        <projectPackages>
            <projectPackage>dev.parfenov.sowa.schema.generator</projectPackage>
            <projectPackage>common.dto</projectPackage>
        </projectPackages>
        <extractDefinitions>false</extractDefinitions>
        <onlyGitDiff>false</onlyGitDiff>
        <branchDiffWith>origin/main</branchDiffWith>
        <stringLengthIncreasePercent>15</stringLengthIncreasePercent>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generateSchema</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Использование

### Автоматическая генерация

Плагин запускается автоматически на фазе `compile`:

```bash
mvn compile
```

## Параметры конфигурации

| Параметр                      | Описание                                                          | Тип | Обязательный | Значение по умолчанию | Возможные значения |
|-------------------------------|-------------------------------------------------------------------|-----|--------------|-----------------------|-------------------|
| `sowaProfileName`             | Имя профиля SOWA для генерации инфраструктурных файлов            | String | Нет          | `SOWA_PROFILE_NAME`   | Любое строковое значение |
| `projectPackages`             | Базовые пакет проекта для поиска REST контроллеров и доп. классов | String | **Да**       | -                     | Полное имя пакета (например, `com.example.project`) |
| `extractDefinitions`          | Извлекать ли определения схем в отдельную секцию                  | Boolean | Нет          | `false`               | `true` / `false` |
| `onlyGitDiff`                 | Обрабатывать только файлы, измененные в Git                       | Boolean | Нет          | `false`               | `true` / `false` |
| `branchDiffWith`              | Ветка для сравнения изменений при использовании `onlyGitDiff`     | String | Нет          | `origin/develop`      | Любое имя ветки Git |
| `stringLengthIncreasePercent` | Процент увеличения длины строковых полей в схемах                 | Integer | Нет          | 0                     | Положительное целое число |

## Примеры

### Базовая конфигурация

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <projectPackages>
        <projectPackage>dev.parfenov.sowa.schema.generator</projectPackage>
        <projectPackage>common.dto</projectPackage>
    </projectPackages>
</configuration>
```

### Конфигурация с Git интеграцией

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <projectPackages>
        <projectPackage>dev.parfenov.sowa.schema.generator</projectPackage>
        <projectPackage>common.dto</projectPackage>
    </projectPackages>
    <onlyGitDiff>true</onlyGitDiff>
    <branchDiffWith>origin/main</branchDiffWith>
</configuration>
```

### Конфигурация с настройкой длины строк

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <projectPackages>
        <projectPackage>dev.parfenov.sowa.schema.generator</projectPackage>
        <projectPackage>common.dto</projectPackage>
    </projectPackages>
    <stringLengthIncreasePercent>15</stringLengthIncreasePercent>
</configuration>
```

## Примеры использования схем

### Работа с типом Object

Плагин предоставляет специальную обработку для типа `Object` в ваших моделях данных.

#### Схема по умолчанию для Object

Если в вашем классе есть поле типа `Object` без дополнительных аннотаций:

```java
@Getter
@Setter
public class ExampleDefault {
    private Object anyObject;
}
```

То для него будет сгенерирована схема по умолчанию:

```json
{
  "anyObject": {
    "type": ["object", "null"],
    "additionalProperties": true
  }
}
```

#### Указание подтипов с помощью аннотаций Jackson

Для более точного определения возможных типов объекта используйте аннотации `@JsonTypeInfo` и `@JsonSubTypes`:

```java
@Getter
@Setter
public class ExampleSubTypes {
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SubType1.class),
            @JsonSubTypes.Type(value = SubType2.class),
    })
    private Object anyObject;
}
```

В этом случае плагин сгенерирует схему, учитывающую указанные подтипы, вместо использования схемы по умолчанию.

#### Указание подтипов для интерфейсов

Аналогично, вы можете использовать те же аннотации на интерфейсах для указания конкретных типов реализаций:

```java
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubType1.class),
        @JsonSubTypes.Type(value = SubType2.class),
})
public interface BaseDto {}
```

Это особенно полезно когда ваши REST контроллеры возвращают интерфейсы, а плагину нужно знать о возможных конкретных реализациях для корректной генерации схемы.

## Поддерживаемые аннотации

Плагин поддерживает следующие аннотации:

- **Spring Web**: `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.
- **Jakarta Validation**: `@NotNull`, `@Size`, `@Pattern`, `@Min`, `@Max`, etc.
- **Swagger/OpenAPI**: `@Schema` для дополнительных метаданных

## Структура выходных файлов

После генерации создаются файлы:
- JSON схемы в директории `target/sowa/request`, `target/sowa/response`
- Инфраструктурные YAML файлы в `target/sowa/services.yml`

## Лицензия

Этот проект распространяется под лицензией MIT. Подробности см. в файле LICENSE.

## Автор

Разработано [Kirill Parfenov](https://github.com/kirillparfenov)

## Поддержка

Если у вас возникли вопросы или проблемы:
1. Проверьте существующие [Issues](https://github.com/kirillparfenov/sowa-schema-generator-maven-plugin/issues)
2. Создайте новый Issue с подробным описанием проблемы
3. Включите информацию о версии Java, Maven и примере кода

