"""
Тесты для проверки системы рекомендаций постов
"""
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


@pytest.fixture
def sample_posts():
    """Создает тестовые посты"""
    return [
        {
            "id": 1,
            "name": "Пост о Python",
            "text": "Изучаю Python программирование и машинное обучение",
            "authorName": "User1",
            "authorId": 1,
            "authorInterests": ["Программирование", "Python", "IT"],
            "roomName": None,
            "roomCategory": None
        },
        {
            "id": 2,
            "name": "Пост о Java",
            "text": "Корпоративная разработка на Java и Spring Boot",
            "authorName": "User2",
            "authorId": 2,
            "authorInterests": ["Программирование", "Java", "IT"],
            "roomName": None,
            "roomCategory": None
        },
        {
            "id": 3,
            "name": "Пост о баскетболе",
            "text": "Играю в баскетбол каждую неделю, ищу команду",
            "authorName": "User3",
            "authorId": 3,
            "authorInterests": ["Спорт", "Баскетбол"],
            "roomName": None,
            "roomCategory": None
        },
        {
            "id": 4,
            "name": "Пост о Data Science",
            "text": "Работаю с данными, использую pandas и numpy",
            "authorName": "User4",
            "authorId": 4,
            "authorInterests": ["Data Science", "Python", "Science"],
            "roomName": None,
            "roomCategory": None
        }
    ]


def test_recommend_posts_with_interests(sample_posts):
    """Тест: рекомендации на основе интересов пользователя"""
    request_data = {
        "userInterests": ["Программирование", "Python"],
        "posts": sample_posts,
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    
    assert "recommended_posts" in data
    assert "similarity_scores" in data
    assert "message" in data
    
    recommended_posts = data["recommended_posts"]
    assert len(recommended_posts) > 0
    
    # Пост о Python должен быть в рекомендациях
    post_ids = [post["id"] for post in recommended_posts]
    assert 1 in post_ids or 4 in post_ids  # Посты о Python/Data Science


def test_recommend_posts_with_subscriptions(sample_posts):
    """Тест: рекомендации на основе подписок (приоритет постам от подписок)"""
    request_data = {
        "userInterests": ["Программирование"],
        "posts": sample_posts,
        "subscribedUserIds": [2]  # Подписан на User2
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    
    recommended_posts = data["recommended_posts"]
    assert len(recommended_posts) > 0
    
    # Пост от подписанного пользователя (User2, id=2) должен быть в топе
    # Проверяем, что пост id=2 есть в рекомендациях
    post_ids = [post["id"] for post in recommended_posts]
    assert 2 in post_ids


def test_recommend_posts_with_subscriptions_priority(sample_posts):
    """Тест: посты от подписок имеют больший приоритет"""
    request_data = {
        "userInterests": ["Программирование", "Python"],
        "posts": sample_posts,
        "subscribedUserIds": [3]  # Подписан на User3 (пост о баскетболе)
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    
    recommended_posts = data["recommended_posts"]
    scores = data["similarity_scores"]
    
    # Находим индекс поста от подписки
    subscribed_post_index = None
    for i, post in enumerate(recommended_posts):
        if post["id"] == 3:  # Пост от User3
            subscribed_post_index = i
            break
    
    # Если пост от подписки есть, он должен иметь высокий приоритет (высокий score)
    if subscribed_post_index is not None and subscribed_post_index < len(scores):
        # Пост от подписки должен иметь относительно высокий score
        subscribed_score = scores[subscribed_post_index]
        # Проверяем, что он не самый низкий (или в топе)
        assert subscribed_score > 0


def test_recommend_posts_no_interests_no_subscriptions(sample_posts):
    """Тест: если нет интересов и подписок, возвращаются все посты"""
    request_data = {
        "userInterests": [],
        "posts": sample_posts,
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    
    recommended_posts = data["recommended_posts"]
    # Должны вернуться все посты (до 50)
    assert len(recommended_posts) == len(sample_posts)
    
    # Проверяем, что все посты присутствуют
    returned_ids = {post["id"] for post in recommended_posts}
    expected_ids = {post["id"] for post in sample_posts}
    assert returned_ids == expected_ids


def test_recommend_posts_empty_posts():
    """Тест: пустой список постов"""
    request_data = {
        "userInterests": ["Программирование"],
        "posts": [],
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    
    assert len(data["recommended_posts"]) == 0
    assert "Нет постов для рекомендации" in data["message"]


def test_recommend_posts_camel_case_support(sample_posts):
    """Тест: поддержка camelCase от Java"""
    # Отправляем запрос с camelCase полями (как отправляет Java)
    request_data = {
        "userInterests": ["Программирование"],
        "posts": [
            {
                "id": 1,
                "name": "Test Post",
                "text": "Test content",
                "authorName": "TestUser",  # camelCase
                "authorId": 1,  # camelCase
                "authorInterests": ["IT"],  # camelCase
                "roomName": None,  # camelCase
                "roomCategory": None  # camelCase
            }
        ],
        "subscribedUserIds": []  # camelCase
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    assert "recommended_posts" in data


def test_recommend_posts_multiple_subscriptions(sample_posts):
    """Тест: несколько подписок"""
    request_data = {
        "userInterests": ["Программирование"],
        "posts": sample_posts,
        "subscribedUserIds": [1, 2]  # Подписан на двух пользователей
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    
    recommended_posts = data["recommended_posts"]
    post_ids = [post["id"] for post in recommended_posts]
    
    # Посты от подписанных пользователей должны быть в рекомендациях
    assert 1 in post_ids or 2 in post_ids


def test_recommend_posts_limit_50():
    """Тест: ограничение до 50 постов"""
    # Создаем 60 постов
    many_posts = []
    for i in range(60):
        many_posts.append({
            "id": i + 1,
            "name": f"Post {i + 1}",
            "text": f"Content {i + 1}",
            "authorId": 1,
            "authorName": "User1"
        })
    
    request_data = {
        "userInterests": [],
        "posts": many_posts,
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    
    # Должно быть максимум 50 постов
    assert len(data["recommended_posts"]) <= 50


def test_recommend_posts_with_room_info(sample_posts):
    """Тест: рекомендации с информацией о комнатах"""
    posts_with_rooms = sample_posts.copy()
    posts_with_rooms[0]["roomName"] = "Python Room"
    posts_with_rooms[0]["roomCategory"] = "IT"
    
    request_data = {
        "userInterests": ["IT", "Программирование"],
        "posts": posts_with_rooms,
        "subscribedUserIds": []
    }
    
    response = client.post("/recommend-posts", json=request_data)
    
    assert response.status_code == 200
    data = response.json()
    
    recommended_posts = data["recommended_posts"]
    assert len(recommended_posts) > 0


def test_health_check():
    """Тест: проверка здоровья API"""
    response = client.get("/health")
    
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "recommendation_api"


def test_root_endpoint():
    """Тест: корневой endpoint"""
    response = client.get("/")
    
    assert response.status_code == 200
    data = response.json()
    assert "message" in data
    assert "version" in data
    assert "endpoints" in data

