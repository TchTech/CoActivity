# Тесты системы рекомендаций

## Описание

Набор тестов для проверки работы системы рекомендаций постов на платформе CoActivity.

## Структура тестов

### Java Backend тесты

1. **PostServiceRecommendationTest** (`src/test/java/com/mipt/CoActivity/service/PostServiceRecommendationTest.java`)
   - Unit тесты для сервиса рекомендаций постов
   - Проверяет логику работы с подписками и интересами
   - Тестирует обработку пустых данных и ошибок

2. **PostRecommendationServiceTest** (`src/test/java/com/mipt/CoActivity/service/PostRecommendationServiceTest.java`)
   - Unit тесты для сервиса взаимодействия с Python API
   - Проверяет обработку HTTP запросов, таймаутов и ошибок
   - Тестирует парсинг ответов от Python API

3. **PostControllerRecommendationTest** (`src/test/java/com/mipt/CoActivity/controller/PostControllerRecommendationTest.java`)
   - Интеграционные тесты через REST API
   - Проверяет endpoint `/posts/recommended`
   - Тестирует различные сценарии использования

### Python FastAPI тесты

1. **test_post_recommendations.py** (`ml/fastapi/test_post_recommendations.py`)
   - Базовые тесты для endpoint `/recommend-posts`
   - Проверяет работу с интересами и подписками
   - Тестирует поддержку camelCase полей от Java
   - Проверяет ограничения и edge cases

2. **test_integration_recommendations.py** (`ml/fastapi/test_integration_recommendations.py`)
   - Интеграционные тесты для полного цикла рекомендаций
   - Проверяет приоритизацию подписок над интересами
   - Тестирует консистентность similarity scores
   - Проверяет правильность сортировки рекомендаций

## Как запустить тесты

### Java тесты

```bash
# Запуск всех тестов
mvn test

# Запуск только тестов рекомендаций
mvn test -Dtest=*Recommendation*

# Запуск конкретного теста
mvn test -Dtest=PostServiceRecommendationTest
```

### Python тесты

```bash
cd ml/fastapi

# Установка зависимостей (если еще не установлены)
pip install -r requirements.txt pytest fastapi[all]

# Запуск всех тестов
pytest -v

# Запуск конкретного теста
pytest test_post_recommendations.py -v

# Запуск с подробным выводом
pytest test_post_recommendations.py -v -s
```

## Что проверяют тесты

### Основные сценарии:

1. **Рекомендации на основе интересов**
   - Пользователь с интересами получает релевантные посты
   - Посты с высоким similarity score идут первыми

2. **Рекомендации на основе подписок**
   - Посты от подписанных пользователей имеют приоритет
   - Подписки имеют больший вес, чем интересы

3. **Комбинация интересов и подписок**
   - Система правильно комбинирует оба источника
   - Приоритет отдается подпискам

4. **Edge cases**
   - Пустой список постов
   - Нет интересов и подписок
   - Нет интересов, но есть подписки
   - Ограничение до 50 постов

5. **Формат данных**
   - Поддержка camelCase от Java backend
   - Правильная структура ответов
   - Консистентность similarity scores

## Проверка работоспособности системы

### 1. Проверка Python API

```bash
cd ml/fastapi
python -m pytest test_post_recommendations.py test_integration_recommendations.py -v
```

### 2. Проверка Java Backend

```bash
mvn test -Dtest=*Recommendation*
```

### 3. Интеграционная проверка

1. Запустить Python API:
   ```bash
   cd ml/fastapi
   uvicorn main:app --reload --port 8000
   ```

2. Запустить Java Backend:
   ```bash
   mvn spring-boot:run
   ```

3. Отправить запрос:
   ```bash
   curl -X GET "http://localhost:8080/posts/recommended?userId=1"
   ```

## Статус тестов

- ✅ Java Unit тесты - готовы
- ✅ Java Integration тесты - готовы
- ✅ Python Unit тесты - готовы
- ✅ Python Integration тесты - готовы

## Рекомендации по улучшению

1. Добавить performance тесты для проверки времени ответа
2. Добавить нагрузочные тесты для проверки стабильности
3. Добавить тесты для проверки кеширования (если будет реализовано)
4. Добавить тесты для проверки обработки больших объемов данных (1000+ постов)

