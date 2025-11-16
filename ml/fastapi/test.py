# ml/fastapi/test.py
import sys
import os
import io

# Установка кодировки UTF-8 для вывода
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

import pytest
from fastapi.testclient import TestClient

# Импорты после добавления пути
from main import app

# Инициализация тестового клиента
client = TestClient(app)

def test_root_endpoint():
    """Тест корневого эндпоинта"""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "message" in data
    assert "version" in data

def test_health_check():
    """Тест проверки здоровья API"""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"

def test_sample_rooms_endpoint():
    """Тест эндпоинта получения примеров комнат"""
    response = client.get("/sample-rooms")
    assert response.status_code == 200
    rooms = response.json()
    assert isinstance(rooms, list)
    assert len(rooms) > 0

def test_recommendations_basic():
    """Базовый тест рекомендаций"""
    request_data = {
        "interests": ["python", "программирование"]
    }

    response = client.post("/recommend-from-sample", json=request_data)
    assert response.status_code == 200
    data = response.json()

    assert "recommended_rooms" in data
    assert "similarity_scores" in data

# Запуск всех тестов
if __name__ == "__main__":
    print("Запуск тестов рекомендательной системы...")

    test_functions = [
        test_root_endpoint,
        test_health_check,
        test_sample_rooms_endpoint,
        test_recommendations_basic,
    ]

    passed_tests = 0
    failed_tests = 0

    for test_func in test_functions:
        try:
            test_func()
            print(f"[OK] {test_func.__name__} - PROYDEN")
            passed_tests += 1
        except Exception as e:
            print(f"[FAIL] {test_func.__name__} - PROVAL: {str(e)}")
            failed_tests += 1

    print(f"\n[RESULT] ITOG: Proydeno: {passed_tests}, Provalen: {failed_tests}, Vsego: {len(test_functions)}")

    if failed_tests == 0:
        print("[SUCCESS] Vse testy proydeny uspeshno!")
        exit(0)
    else:
        print("[WARNING] Nekotorye testy provalilis!")
        exit(1)