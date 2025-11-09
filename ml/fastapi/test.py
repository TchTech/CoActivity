# test.py
import pytest
import requests
import json
from main import app, RecommendationService, sample_rooms
from fastapi.testclient import TestClient
from main import Room, UserInterests, RecommendationRequest

# Инициализация тестового клиента - исправленная версия
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

    assert len(data["recommended_rooms"]) > 0

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

def test_empty_interests():
    """Тест с пустыми интересами"""
    request_data = {
        "interests": []
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 400

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

    assert "recommended_rooms" in data
    assert "similarity_scores" in data
    assert "message" in data
    assert len(data["recommended_rooms"]) == len(data["similarity_scores"])

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
        test_empty_interests,
        test_similarity_scores_range,
        test_recommendations_structure,
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
        exit(0)
    else:
        print("⚠️  Некоторые тесты провалились!")
        exit(1)