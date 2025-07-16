# SOWA Schema Generator Maven Plugin

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
        <sowaProfileName>gigacensor</sowaProfileName>
        <projectPackage>dev.parfenov.sowa.schema.generator</projectPackage>
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

### Ручной запуск

```bash
mvn sowa-schema-generator:generateSchema
```

### Генерация только для измененных файлов

```bash
mvn sowa-schema-generator:generateSchema -DonlyGitDiff=true
```

## Параметры конфигурации

| Параметр | Описание | Тип | Обязательный | Значение по умолчанию | Возможные значения |
|----------|----------|-----|--------------|----------------------|-------------------|
| `sowaProfileName` | Имя профиля SOWA для генерации инфраструктурных файлов | String | Нет | `SOWA_PROFILE_NAME` | Любое строковое значение |
| `projectPackage` | Базовый пакет проекта для поиска REST контроллеров | String | **Да** | - | Полное имя пакета (например, `com.example.project`) |
| `extractDefinitions` | Извлекать ли определения схем в отдельную секцию | Boolean | **Да** | - | `true` / `false` |
| `onlyGitDiff` | Обрабатывать только файлы, измененные в Git | Boolean | Нет | `false` | `true` / `false` |
| `branchDiffWith` | Ветка для сравнения изменений при использовании `onlyGitDiff` | String | Нет | `origin/develop` | Любое имя ветки Git |
| `stringLengthIncreasePercent` | Процент увеличения длины строковых полей в схемах | Integer | Нет | - | Положительное целое число |

## Примеры

### Базовая конфигурация

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <extractDefinitions>false</extractDefinitions>
</configuration>
```

### Конфигурация с Git интеграцией

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <extractDefinitions>true</extractDefinitions>
    <onlyGitDiff>true</onlyGitDiff>
    <branchDiffWith>origin/develop</branchDiffWith>
</configuration>
```

### Конфигурация с настройкой длины строк

```xml
<configuration>
    <projectPackage>com.example.myapp</projectPackage>
    <extractDefinitions>false</extractDefinitions>
    <stringLengthIncreasePercent>15</stringLengthIncreasePercent>
</configuration>
```

## Поддерживаемые аннотации

Плагин поддерживает следующие аннотации:

- **Spring Web**: `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.
- **Jakarta Validation**: `@NotNull`, `@Size`, `@Pattern`, `@Min`, `@Max`, etc.
- **Swagger/OpenAPI**: `@Schema`, `@ArraySchema` для дополнительных метаданных

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

