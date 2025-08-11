# SOWA Schema Generator Plugin

[![Java CI with Maven](https://github.com/kirillparfenov/sowa-schema-generator-maven-plugin/actions/workflows/maven.yml/badge.svg)](https://github.com/kirillparfenov/sowa-schema-generator-maven-plugin/actions/workflows/maven.yml)
[![CodeQL Advanced](https://github.com/kirillparfenov/sowa-schema-generator-maven-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/kirillparfenov/sowa-schema-generator-maven-plugin/actions/workflows/codeql.yml)

Универсальный инструмент для автоматической генерации JSON схем из Spring REST контроллеров для системы SOWA. Поддерживает Maven, Gradle и CLI.

## Описание

Плагин анализирует ваши Spring REST контроллеры и автоматически генерирует:
- JSON схемы для валидации запросов и ответов
- Инфраструктурные файлы для интеграции с SOWA
- Поддержка Jakarta Validation аннотаций
- Интеграция с Git для обработки только измененных файлов

## Требования

- Java 17+
- Maven 3.6+ (для Maven плагина)
- Gradle 6.0+ (для Gradle плагина)
- Spring Framework (для REST контроллеров)

## Установка

### Шаг 1: Клонирование и сборка плагина

Сначала склонируйте репозиторий и установите плагин в локальный Maven репозиторий:

```bash
# Клонируйте репозиторий
git clone https://github.com/kirillparfenov/sowa-schema-plugin.git
cd sowa-schema-plugin

# Установите плагин в локальный .m2 репозиторий
mvn clean install
```

⚠️ **Важно:** Этот шаг обязателен! Без выполнения `mvn install` плагин не будет доступен в локальном Maven репозитории, и вы получите ошибки при попытке использования в своих проектах.

### Шаг 2: Использование в проектах

После успешной установки плагин будет доступен в локальном Maven репозитории (`~/.m2/repository/`) и готов к использованию в ваших проектах.

Выберите один из способов использования в зависимости от вашей системы сборки:

## Использование

### Maven Plugin

#### Конфигурация

Добавьте плагин в ваш `pom.xml`:

```xml
<plugin>
    <groupId>dev.parfenov</groupId>
    <artifactId>sowa-schema-generator-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <sowaProfileName>super_profile_name</sowaProfileName>
        <projectPackages>
            <projectPackage>com.example.project</projectPackage>
            <projectPackage>common.dto.package</projectPackage>
        </projectPackages>
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

#### Запуск

```bash
mvn compile
```

### Gradle Plugin

#### Конфигурация

Добавьте в ваш `build.gradle`:

```gradle
buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath 'dev.parfenov:sowa-schema-generator-maven-plugin:1.0.0'
    }
}

apply plugin: dev.parfenov.sowa.schema.plugin.SowaGradle
sowaSchema {
    projectPackages = ['com.example.package', 'common.dto.package']
    sowaProfileName = 'super_profile_name'
}
```

#### Запуск

```bash
./gradlew generateSchema
```

### CLI (Command Line Interface)

#### Запуск

```bash
java -DprojectPackages=com.example.package,common.dto.package \
  -DsowaProfileName=super_profile_name \
  -DuberJarPath=/path/to/your/application.jar \
  -jar sowa-schema-plugin.jar
```

## Параметры конфигурации

Следующие параметры применимы во всех трех режимах использования (Maven, Gradle, CLI):

| Параметр                      | Описание                                                                         | Тип     | Обязательный | Значение по умолчанию | Возможные значения                                  |
|-------------------------------|----------------------------------------------------------------------------------|---------|--------------|-----------------------|-----------------------------------------------------|
| `sowaProfileName`             | Имя профиля SOWA для генерации инфраструктурных файлов                           | String  | Нет          | `SOWA_PROFILE_NAME`   | Любое строковое значение                            |
| `projectPackages`             | Базовые пакет проекта для поиска REST контроллеров и доп. классов                | String  | **Да**       | -                     | Полное имя пакета (например, `com.example.project`) |
| `extractDefinitions`          | Извлекать ли определения схем в отдельную секцию                                 | Boolean | Нет          | `false`               | `true` / `false`                                    |
| `onlyGitDiff`                 | Обрабатывать только файлы, измененные в Git                                      | Boolean | Нет          | `false`               | `true` / `false`                                    |
| `branchDiffWith`              | Ветка для сравнения изменений при использовании `onlyGitDiff`                    | String  | Нет          | `origin/develop`      | Любое имя ветки Git                                 |
| `stringLengthIncreasePercent` | Процент увеличения длины строковых полей в схемах                                | Integer | Нет          | 0                     | Положительное целое число                           |
| `uberJarPath`                 | Путь к uber-jar приложению, которое нужно сканировать. **(ПАРАМЕТР ТОЛЬКО ДЛЯ CLI)** | String  | **Да (для cli)** | -                     | Абсолютный путь до uber-jar в системе               |

## Примеры

### Базовая конфигурация

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <projectPackages>
        <projectPackage>com.example.package</projectPackage>
        <projectPackage>common.dto.package</projectPackage>
    </projectPackages>
</configuration>
```

### Конфигурация с Git интеграцией

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <projectPackages>
        <projectPackage>com.example.package</projectPackage>
        <projectPackage>common.dto.package</projectPackage>
    </projectPackages>
    <onlyGitDiff>true</onlyGitDiff>
    <branchDiffWith>origin/main</branchDiffWith>
</configuration>
```
```gradle
sowaSchema {
    projectPackages = ['com.example.package', 'common.dto.package']
    sowaProfileName = 'super_profile_name'
    onlyGitDiff = true
    branchDiffWith = 'origin/main'
}
```

### Конфигурация с настройкой длины строк

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <projectPackages>
        <projectPackage>com.example.package</projectPackage>
        <projectPackage>common.dto.package</projectPackage>
    </projectPackages>
    <stringLengthIncreasePercent>15</stringLengthIncreasePercent>
</configuration>
```
```gradle
sowaSchema {
    projectPackages = ['com.example.package', 'common.dto.package']
    sowaProfileName = 'super_profile_name'
    stringLengthIncreasePercent = 15
}
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

