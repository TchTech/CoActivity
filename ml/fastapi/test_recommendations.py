"""
Тесты для системы рекомендаций постов в FastAPI
"""
import pytest
from fastapi.testclient import TestClient
from main import app, recommendation_service, Post, PostRecommendationRequest

client = TestClient(app)


def test_health_check():
    """Тест проверки здоровья API"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "healthy", "service": "recommendation_api"}


def test_recommend_posts_with_interests():
    """Тест рекомендаций на основе интересов пользователя"""
    request_data = {
        "userInterests": ["Программирование", "Python"],
        "posts": [
            {
                "id": 1,
                "name": "Post about Python",
                "text": "This is a post about Python programming",
                "authorId": 1,
                "authorName": "User1"
            },
            {
                "id": 2,
                "name": "Post about Cooking",
                "text": "This is a post about cooking recipes",
                "authorId": 2,
                "authorName": "User2"
            }
        ],
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert "recommendedPosts" in data
    assert "similarityScores" in data
    assert len(data["recommendedPosts"]) > 0
    # Пост о Python должен быть выше поста о готовке
    assert data["recommendedPosts"][0]["id"] == 1


def test_recommend_posts_with_subscriptions():
    """Тест рекомендаций на основе подписок"""
    request_data = {
        "userInterests": ["Программирование"],
        "posts": [
            {
                "id": 1,
                "name": "Post from subscribed user",
                "text": "This is a post from a subscribed user",
                "authorId": 2,
                "authorName": "SubscribedUser"
            },
            {
                "id": 2,
                "name": "Post from non-subscribed user",
                "text": "This is a post from a non-subscribed user",
                "authorId": 3,
                "authorName": "NonSubscribedUser"
            }
        ],
        "subscribedUserIds": [2]
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert len(data["recommendedPosts"]) > 0
    # Пост от подписанного пользователя должен быть первым
    assert data["recommendedPosts"][0]["id"] == 1
    assert data["recommendedPosts"][0]["authorId"] == 2


def test_recommend_posts_no_interests_no_subscriptions():
    """Тест рекомендаций без интересов и подписок"""
    request_data = {
        "userInterests": [],
        "posts": [
            {
                "id": 1,
                "name": "Post 1",
                "text": "Content 1",
                "authorId": 1,
                "authorName": "User1"
            },
            {
                "id": 2,
                "name": "Post 2",
                "text": "Content 2",
                "authorId": 2,
                "authorName": "User2"
            }
        ],
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert len(data["recommendedPosts"]) <= 50  # Ограничение до 50 постов
    assert len(data["recommendedPosts"]) == len(data["similarityScores"])


def test_recommend_posts_empty_posts():
    """Тест с пустым списком постов"""
    request_data = {
        "userInterests": ["Программирование"],
        "posts": [],
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert data["recommendedPosts"] == []
    assert data["similarityScores"] == []
    assert "Нет постов для рекомендации" in data["message"]


def test_recommend_posts_with_room_info():
    """Тест рекомендаций с информацией о комнате"""
    request_data = {
        "userInterests": ["IT", "Программирование"],
        "posts": [
            {
                "id": 1,
                "name": "Post about Python",
                "text": "Python programming",
                "authorId": 1,
                "authorName": "User1",
                "authorInterests": ["Python", "IT"],
                "roomName": "Python Learning Room",
                "roomCategory": "IT"
            },
            {
                "id": 2,
                "name": "Post about Cooking",
                "text": "Cooking recipes",
                "authorId": 2,
                "authorName": "User2",
                "authorInterests": ["Cooking"],
                "roomName": "Cooking Room",
                "roomCategory": "Food"
            }
        ],
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert len(data["recommendedPosts"]) > 0
    # Пост о Python должен быть выше поста о готовке
    assert data["recommendedPosts"][0]["id"] == 1


def test_recommend_posts_prioritizes_subscriptions():
    """Тест приоритизации постов от подписок"""
    request_data = {
        "userInterests": ["Cooking"],
        "posts": [
            {
                "id": 1,
                "name": "Tech post from subscribed user",
                "text": "Technology post",
                "authorId": 2,
                "authorName": "SubscribedUser"
            },
            {
                "id": 2,
                "name": "Cooking post from non-subscribed",
                "text": "Cooking post matching interests",
                "authorId": 3,
                "authorName": "NonSubscribedUser"
            }
        ],
        "subscribedUserIds": [2]
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert len(data["recommendedPosts"]) > 0
    # Пост от подписанного пользователя должен быть выше, даже если он не соответствует интересам
    assert data["recommendedPosts"][0]["id"] == 1
    assert data["recommendedPosts"][0]["authorId"] == 2


def test_recommend_posts_multiple_subscriptions():
    """Тест с несколькими подписками"""
    request_data = {
        "userInterests": ["Программирование"],
        "posts": [
            {
                "id": 1,
                "name": "Post from user 2",
                "text": "Content from subscribed user 2",
                "authorId": 2,
                "authorName": "User2"
            },
            {
                "id": 2,
                "name": "Post from user 3",
                "text": "Content from subscribed user 3",
                "authorId": 3,
                "authorName": "User3"
            },
            {
                "id": 3,
                "name": "Post from non-subscribed",
                "text": "Content from non-subscribed user",
                "authorId": 4,
                "authorName": "User4"
            }
        ],
        "subscribedUserIds": [2, 3]
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert len(data["recommendedPosts"]) > 0
    # Первые два поста должны быть от подписанных пользователей
    first_two_authors = [post["authorId"] for post in data["recommendedPosts"][:2]]
    assert 2 in first_two_authors
    assert 3 in first_two_authors


def test_recommend_posts_limit_50():
    """Тест ограничения до 50 постов"""
    # Создаем 100 постов
    posts = []
    for i in range(100):
        posts.append({
            "id": i + 1,
            "name": f"Post {i + 1}",
            "text": f"Content {i + 1}",
            "authorId": 1,
            "authorName": "User1"
        })
    
    request_data = {
        "userInterests": ["Программирование"],
        "posts": posts,
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert len(data["recommendedPosts"]) <= 50
    assert len(data["similarityScores"]) <= 50


def test_recommend_posts_camel_case_fields():
    """Тест обработки camelCase полей от Java"""
    request_data = {
        "userInterests": ["IT"],
        "posts": [
            {
                "id": 1,
                "name": "Post",
                "text": "Content",
                "authorId": 1,
                "authorName": "User",
                "authorInterests": ["Python"],
                "roomName": "Room",
                "roomCategory": "IT"
            }
        ],
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert len(data["recommendedPosts"]) > 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])

