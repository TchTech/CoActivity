# Руководство по запуску тестов и отслеживанию покрытия кода

## Быстрый старт

### 1. Запуск всех тестов
```bash
mvn test
```

### 2. Запуск тестов с генерацией отчета о покрытии
```bash
mvn clean test jacoco:report
```

### 3. Просмотр отчета о покрытии
После выполнения команды выше, откройте в браузере:
```
target/site/jacoco/index.html
```

## Детальные команды

### Запуск конкретных тестов

#### Запуск тестов конкретного класса:
```bash
mvn test -Dtest=PostServiceTest
```

#### Запуск нескольких классов:
```bash
mvn test -Dtest=PostServiceTest,CommentServiceTest,UserServiceTest
```

#### Запуск всех тестов в пакете:
```bash
mvn test -Dtest=com.mipt.CoActivity.service.*Test
```

#### Исключение определенных тестов:
```bash
mvn test -Dtest='!*ControllerTest'
```

### Генерация отчетов о покрытии

#### Базовый отчет:
```bash
mvn clean test jacoco:report
```

#### Отчет с проверкой минимального покрытия (60%):
```bash
mvn clean test jacoco:check
```

#### Полный отчет с детальной информацией:
```bash
mvn clean test jacoco:report jacoco:check
```

### Просмотр результатов

#### HTML отчет:
1. Выполните: `mvn clean test jacoco:report`
2. Откройте файл: `target/site/jacoco/index.html` в браузере
3. В отчете вы увидите:
   - Общее покрытие по пакетам
   - Покрытие по классам
   - Покрытие по методам
   - Покрытие по строкам кода
   - Покрытие по ветвлениям (branches)

#### CSV отчет:
```bash
mvn clean test jacoco:report
# Результаты в: target/site/jacoco/jacoco.csv
```

#### XML отчет (для CI/CD):
```bash
mvn clean test jacoco:report
# Результаты в: target/site/jacoco/jacoco.xml
```

## Интерпретация метрик покрытия

### Метрики JaCoCo:

1. **Line Coverage (Покрытие строк)**
   - Процент выполненных строк кода
   - Цель: ≥ 60%

2. **Branch Coverage (Покрытие ветвлений)**
   - Процент покрытых условных ветвлений (if/else, switch)
   - Цель: ≥ 60%

3. **Method Coverage (Покрытие методов)**
   - Процент вызванных методов
   - Цель: ≥ 60%

4. **Class Coverage (Покрытие классов)**
   - Процент классов, в которых выполнены тесты
   - Цель: ≥ 60%

## Примеры использования

### Проверка покрытия перед коммитом:
```bash
mvn clean test jacoco:check
```

### Детальный анализ покрытия конкретного сервиса:
```bash
# Запустить тесты только для PostService
mvn test -Dtest=PostServiceTest jacoco:report

# Открыть отчет
open target/site/jacoco/index.html
```

### Поиск непокрытых участков кода:
1. Откройте HTML отчет
2. Кликните на пакет/класс
3. Красным цветом будут выделены непокрытые строки
4. Желтым - частично покрытые ветвления

## Настройка минимального покрытия

Минимальное покрытие настроено в `pom.xml`:
- **Line Coverage**: 60%
- **Branch Coverage**: 60%
- **Method Coverage**: 60%

Если покрытие ниже, сборка завершится с ошибкой.

## Полезные команды

### Очистка и пересборка:
```bash
mvn clean test
```

### Запуск тестов с подробным выводом:
```bash
mvn test -X
```

### Запуск тестов с выводом в консоль:
```bash
mvn test | grep -E "(Tests run:|BUILD)"
```

### Пропуск тестов (только компиляция):
```bash
mvn clean compile -DskipTests
```

## Интеграция с IDE

### IntelliJ IDEA:
1. Правый клик на тестовый класс → "Run 'TestName'"
2. После выполнения: View → Tool Windows → Coverage
3. Или: Run → Show Coverage Data

### VS Code:
- Установите расширение "Java Test Runner"
- Используйте команду: `Java: Run Tests`

## CI/CD интеграция

Для автоматической проверки покрытия в CI/CD:

```yaml
# Пример для GitHub Actions
- name: Run tests with coverage
  run: mvn clean test jacoco:report

- name: Upload coverage report
  uses: codecov/codecov-action@v3
  with:
    file: target/site/jacoco/jacoco.xml
```

## Решение проблем

### Тесты не запускаются:
```bash
# Проверьте компиляцию
mvn clean compile test-compile

# Проверьте зависимости
mvn dependency:tree
```

### Отчет не генерируется:
```bash
# Убедитесь, что тесты выполнены
mvn clean test

# Затем сгенерируйте отчет
mvn jacoco:report
```

### Низкое покрытие:
1. Откройте HTML отчет
2. Найдите классы с низким покрытием
3. Добавьте тесты для непокрытых методов
4. Перезапустите тесты

## Дополнительные ресурсы

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

