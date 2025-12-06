# Быстрый старт: Запуск тестов и просмотр покрытия

## 🚀 Основные команды

### 1. Запустить все тесты
```bash
mvn test
```

### 2. Запустить тесты и сгенерировать отчет о покрытии
```bash
mvn clean test jacoco:report
```

### 3. Открыть отчет о покрытии в браузере
После выполнения команды выше:
```bash
# macOS
open target/site/jacoco/index.html

# Linux
xdg-open target/site/jacoco/index.html

# Windows
start target/site/jacoco/index.html
```

## 📊 Что показывает отчет

В HTML отчете вы увидите:
- **Общее покрытие** по пакетам (controller, service, exception и т.д.)
- **Покрытие по классам** - какие классы покрыты тестами
- **Покрытие по методам** - процент протестированных методов
- **Покрытие по строкам** - процент выполненных строк кода
- **Покрытие по ветвлениям** - процент покрытых if/else, switch и т.д.

### Цветовая индикация:
- 🟢 **Зеленый** - полностью покрыто
- 🟡 **Желтый** - частично покрыто
- 🔴 **Красный** - не покрыто

## 🎯 Запуск конкретных тестов

### Только успешные тесты:
```bash
mvn test -Dtest=PostServiceTest,CommentServiceTest,UserServiceTest,GlobalExceptionHandlerTest,AuthServiceTest,ImageServiceTest
```

### С отчетом о покрытии:
```bash
mvn test -Dtest=PostServiceTest,CommentServiceTest,UserServiceTest,GlobalExceptionHandlerTest,AuthServiceTest,ImageServiceTest jacoco:report
```

## 📈 Проверка минимального покрытия (60%)

```bash
mvn clean test jacoco:check
```

Если покрытие ниже 60%, команда завершится с ошибкой.

## 🔍 Поиск непокрытого кода

1. Откройте `target/site/jacoco/index.html`
2. Кликните на пакет (например, `service`)
3. Кликните на класс
4. Вы увидите исходный код с цветовой маркировкой:
   - **Красные строки** - не выполнены
   - **Желтые строки** - частично выполнены (не все ветвления)
   - **Зеленые строки** - полностью покрыты

## 📝 Примеры использования

### Проверить покрытие перед коммитом:
```bash
mvn clean test jacoco:check
```

### Посмотреть детальное покрытие PostService:
```bash
mvn test -Dtest=PostServiceTest jacoco:report
open target/site/jacoco/index.html
```

### Запустить только быстрые unit-тесты (без интеграционных):
```bash
mvn test -Dtest='!*ControllerTest' jacoco:report
```

## ⚙️ Настройки покрытия

Минимальное покрытие настроено в `pom.xml`:
- **Line Coverage**: 60%
- **Branch Coverage**: 60%  
- **Method Coverage**: 60%

## 📁 Расположение отчетов

После выполнения `mvn jacoco:report`:
- **HTML**: `target/site/jacoco/index.html`
- **XML**: `target/site/jacoco/jacoco.xml` (для CI/CD)
- **CSV**: `target/site/jacoco/jacoco.csv`

## 🐛 Решение проблем

### Тесты не запускаются:
```bash
# Очистить и пересобрать
mvn clean compile test-compile
```

### Отчет не генерируется:
```bash
# Сначала запустите тесты
mvn test

# Затем сгенерируйте отчет
mvn jacoco:report
```

### Нужно пропустить падающие тесты:
```bash
# Запустить только успешные тесты
mvn test -Dtest=PostServiceTest,CommentServiceTest,UserServiceTest jacoco:report
```

## 📚 Подробная документация

См. `TEST_COVERAGE_GUIDE.md` для полной документации.

