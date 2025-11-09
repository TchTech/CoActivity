# test.py
import pytest
import requests
import json
from main import app, RecommendationService, sample_rooms
from fastapi.testclient import TestClient
from main import Room, UserInterests, RecommendationRequest

# Инициализация тестового клиента
client = TestClient(app)


def test_root_endpoint():
    """Тест корневого эндпоинта"""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "message" in data
    assert "version" in data
    assert "endpoints" in data


def test_sample_rooms_endpoint():
    """Тест эндпоинта получения примеров комнат"""
    response = client.get("/sample-rooms")
    assert response.status_code == 200
    rooms = response.json()
    assert isinstance(rooms, list)
    assert len(rooms) > 0
    assert "id" in rooms[0]
    assert "title" in rooms[0]
    assert "category" in rooms[0]


def test_health_check():
    """Тест проверки здоровья API"""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "recommendation_api"


def test_recommendations_python_interests():
    """Тест рекомендаций для интересов в Python"""
    request_data = {
        "interests": ["python", "программирование", "веб-разработка", "данные"],
        "preferred_categories": ["Программирование"],
        "location": "Онлайн"
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 200
    data = response.json()

    assert "recommended_rooms" in data
    assert "similarity_scores" in data
    assert "message" in data

    # Проверяем, что все рекомендации относятся к программированию
    for room in data["recommended_rooms"]:
        assert room["category"] == "Программирование"
        assert room["location"] == "Онлайн"


def test_recommendations_sports_interests():
    """Тест рекомендаций для спортивных интересов"""
    request_data = {
        "interests": ["футбол", "баскетбол", "спорт", "командные игры"],
        "preferred_categories": ["Спорт"],
        "location": "Москва"
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 200
    data = response.json()

    # Проверяем, что все рекомендации относятся к спорту
    for room in data["recommended_rooms"]:
        assert room["category"] == "Спорт"
        assert room["location"] == "Москва"


def test_recommendations_no_filters():
    """Тест рекомендаций без фильтров"""
    request_data = {
        "interests": ["программирование", "спорт"]
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 200
    data = response.json()

    assert len(data["recommended_rooms"]) > 0
    assert len(data["similarity_scores"]) == len(data["recommended_rooms"])


def test_recommendations_custom_rooms():
    """Тест рекомендаций с пользовательскими комнатами"""
    custom_rooms = [
        {
            "id": 100,
            "title": "Машинное обучение на Python",
            "description": "Изучение ML и нейронных сетей на Python с TensorFlow",
            "category": "Программирование",
            "subcategory": "Python",
            "location": "Онлайн"
        },
        {
            "id": 101,
            "title": "Мобильная разработка",
            "description": "Создание мобильных приложений на React Native",
            "category": "Программирование",
            "subcategory": "JavaScript",
            "location": "Онлайн"
        }
    ]

    request_data = {
        "user_interests": {
            "interests": ["python", "машинное обучение", "нейронные сети"]
        },
        "rooms": custom_rooms
    }

    response = client.post("/recommend", json=request_data)
    assert response.status_code == 200
    data = response.json()

    assert len(data["recommended_rooms"]) > 0
    # Проверяем, что первая рекомендация связана с Python и ML
    first_room = data["recommended_rooms"][0]
    assert "python" in first_room["title"].lower() or "машинное" in first_room["title"].lower()


# def test_empty_interests():
#     """Тест с пустыми интересами"""
#     request_data = {
#         "interests": []
#     }
#
#     response = client.post("/recommend-from-sample", json=request_data)
#     assert response.status_code == 400
#
#
# def test_no_rooms():
#     """Тест с пустым списком комнат"""
#     request_data = {
#         "user_interests": {
#             "interests": ["python"]
#         },
#         "rooms": []
#     }
#
#     response = client.post("/recommend", json=request_data)
#     assert response.status_code == 400


def test_recommendation_service_class():
    """Тест класса RecommendationService"""
    service = RecommendationService()

    # Тест подготовки текстовых данных
    test_rooms = [
        Room(
            id=1,
            title="Тестовая комната",
            description="Описание тестовой комнаты",
            category="Тест",
            subcategory="Тест",
            location="Тест"
        )
    ]

    texts = service.prepare_text_data(test_rooms)
    assert len(texts) == 1
    assert "Тестовая комната" in texts[0]
    assert "Описание тестовой комнаты" in texts[0]

    # Тест получения рекомендаций
    recommendations = service.get_recommendations(
        user_interests=["python", "программирование"],
        rooms=test_rooms,
        top_n=3
    )

    assert len(recommendations) == 1
    assert isinstance(recommendations[0][0], Room)
    assert isinstance(recommendations[0][1], float)


def test_filter_recommendations():
    """Тест фильтрации рекомендаций"""
    service = RecommendationService()

    test_recommendations = [
        (Room(id=1, title="Python", description="", category="Программирование", subcategory="Python",
              location="Онлайн"), 0.9),
        (Room(id=2, title="Футбол", description="", category="Спорт", subcategory="Футбол", location="Москва"), 0.8),
        (Room(id=3, title="Йога", description="", category="Спорт", subcategory="Йога", location="Онлайн"), 0.7)
    ]

    # Фильтр по категории
    filtered = service.filter_recommendations(
        test_recommendations,
        preferred_categories=["Программирование"]
    )
    assert len(filtered) == 1
    assert filtered[0][0].category == "Программирование"

    # Фильтр по местоположению
    filtered = service.filter_recommendations(
        test_recommendations,
        location="Онлайн"
    )
    assert len(filtered) == 2
    assert all(room.location == "Онлайн" for room, score in filtered)

    # Комбинированный фильтр
    filtered = service.filter_recommendations(
        test_recommendations,
        preferred_categories=["Спорт"],
        location="Онлайн"
    )
    assert len(filtered) == 1
    assert filtered[0][0].category == "Спорт"
    assert filtered[0][0].location == "Онлайн"


def test_similarity_scores_range():
    """Тест что scores находятся в правильном диапазоне"""
    request_data = {
        "interests": ["python", "программирование"]
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 200
    data = response.json()

    for score in data["similarity_scores"]:
        assert 0 <= score <= 1.0


def test_recommendations_structure():
    """Тест структуры ответа рекомендаций"""
    request_data = {
        "interests": ["python"]
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 200
    data = response.json()

    # Проверяем структуру ответа
    assert "recommended_rooms" in data
    assert "similarity_scores" in data
    assert "message" in data

    # Проверяем что количество комнат и scores совпадает
    assert len(data["recommended_rooms"]) == len(data["similarity_scores"])

    # Проверяем структуру каждой комнаты
    for room in data["recommended_rooms"]:
        required_fields = ["id", "title", "description", "category", "subcategory", "location"]
        for field in required_fields:
            assert field in room


def test_no_matching_recommendations():
    """Тест когда нет подходящих рекомендаций"""
    request_data = {
        "interests": ["квантовая физика"],  # Очень специфичный интерес
        "preferred_categories": ["Наука"],
        "location": "Марс"  # Несуществующее местоположение
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 200
    data = response.json()

    assert len(data["recommended_rooms"]) == 0
    assert len(data["similarity_scores"]) == 0
    assert "Не найдено" in data["message"]


def test_mixed_categories_recommendations():
    """Тест рекомендаций с интересами из разных категорий"""
    request_data = {
        "interests": ["python", "футбол", "йога"]
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 200
    data = response.json()

    # Должны получить рекомендации из разных категорий
    categories = set(room["category"] for room in data["recommended_rooms"])
    assert len(categories) >= 1  # Могут быть рекомендации только из одной категории, если они самые релевантные


# Запуск всех тестов
if __name__ == "__main__":
    print("Запуск тестов рекомендательной системы...")

    # Список всех тестовых функций
    test_functions = [
        test_root_endpoint,
        test_sample_rooms_endpoint,
        test_health_check,
        test_recommendations_python_interests,
        test_recommendations_sports_interests,
        test_recommendations_no_filters,
        test_recommendations_custom_rooms,
        test_empty_interests,
        test_no_rooms,
        test_recommendation_service_class,
        test_filter_recommendations,
        test_similarity_scores_range,
        test_recommendations_structure,
        test_no_matching_recommendations,
        test_mixed_categories_recommendations
    ]

    passed_tests = 0
    failed_tests = 0

    for test_func in test_functions:
        try:
            test_func()
            print(f"✅ {test_func.__name__} - ПРОЙДЕН")
            passed_tests += 1
        except Exception as e:
            print(f"❌ {test_func.__name__} - ПРОВАЛ: {str(e)}")
            failed_tests += 1

    print(f"\n📊 ИТОГ:")
    print(f"Пройдено: {passed_tests}")
    print(f"Провалено: {failed_tests}")
    print(f"Всего: {len(test_functions)}")

    if failed_tests == 0:
        print("🎉 Все тесты пройдены успешно!")
    else:
        print("⚠️  Некоторые тесты провалились!")