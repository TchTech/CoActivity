import pytest
from fastapi.testclient import TestClient
from main import app, sample_rooms, UserInterests, Room, RecommendationRequest

client = TestClient(app)

def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    json_data = response.json()
    assert "message" in json_data
    assert json_data["version"] == "1.0.1"

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"

def test_sample_rooms():
    response = client.get("/sample-rooms")
    assert response.status_code == 200
    rooms = response.json()
    assert isinstance(rooms, list)
    assert len(rooms) == len(sample_rooms)
    assert all("id" in room for room in rooms)

def test_recommendations_success():
    data = {
        "user_interests": {
            "interests": ["Python", "Программирование"],
            "preferred_categories": ["IT"],
            "location": "Москва"
        },
        "rooms": [room.dict() for room in sample_rooms],
        "history_of_users_rooms": []
    }
    response = client.post("/recommend", json=data)
    assert response.status_code == 200
    json_data = response.json()
    assert "recommended_rooms" in json_data
    assert isinstance(json_data["recommended_rooms"], list)
    assert "similarity_scores" in json_data
    assert len(json_data["recommended_rooms"]) == len(json_data["similarity_scores"])
    assert json_data["message"].startswith("Найдено")

def test_recommendations_no_interests():
    data = {
        "user_interests": {
            "interests": [],
            "preferred_categories": ["IT"],
            "location": "Москва"
        },
        "rooms": [room.dict() for room in sample_rooms],
        "history_of_users_rooms": []
    }
    response = client.post("/recommend", json=data)
    assert response.status_code == 400
    assert response.json()["detail"] == "Интересы пользователя не могут быть пустыми"

def test_recommendations_no_rooms():
    data = {
        "user_interests": {
            "interests": ["Python"],
            "preferred_categories": ["IT"],
            "location": "Москва"
        },
        "rooms": [],
        "history_of_users_rooms": []
    }
    response = client.post("/recommend", json=data)
    assert response.status_code == 400
    assert response.json()["detail"] == "Список комнат не может быть пустым"

def test_recommendations_from_sample():
    user_interests = {
        "interests": ["Баскетбол"],
        "preferred_categories": ["Sport"],
        "location": "Москва"
    }
    response = client.post("/recommend-from-sample", json=user_interests)
    assert response.status_code == 200
    data = response.json()
    assert "recommended_rooms" in data
    assert len(data["recommended_rooms"]) <= 5

def test_filtering_by_category_geoposition():
    user_interests = {
        "interests": ["Йога"],
        "preferred_categories": ["LifeStyle"],
        "location": "Москва"
    }
    response = client.post("/recommend-from-sample", json=user_interests)
    assert response.status_code == 200
    for room in response.json()["recommended_rooms"]:
        assert room["interest_type"] == "LifeStyle"
        assert room["geoposition"] == "Москва"

