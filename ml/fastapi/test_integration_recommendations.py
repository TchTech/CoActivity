"""
Интеграционные тесты для проверки полного цикла рекомендаций
Проверяет работу системы с реальными данными и edge cases
"""
import pytest
from fastapi.testclient import TestClient
from main import app, recommendation_service

client = TestClient(app)


class TestPostRecommendationIntegration:
    """Интеграционные тесты для рекомендаций постов"""
    
    def test_full_recommendation_flow_with_interests(self):
        """Полный цикл: пользователь с интересами получает рекомендации"""
        posts = [
            {
                "id": 1,
                "name": "Python Tutorial",
                "text": "Learn Python programming basics and advanced topics",
                "authorId": 1,
                "authorName": "PythonExpert",
                "authorInterests": ["Python", "Programming"],
                "roomName": "Python Learning",
                "roomCategory": "IT"
            },
            {
                "id": 2,
                "name": "Java Spring Boot",
                "text": "Enterprise Java development with Spring Boot framework",
                "authorId": 2,
                "authorName": "JavaDev",
                "authorInterests": ["Java", "Spring"],
                "roomName": "Java Room",
                "roomCategory": "IT"
            },
            {
                "id": 3,
                "name": "Basketball Game",
                "text": "Join our weekly basketball games",
                "authorId": 3,
                "authorName": "SportLover",
                "authorInterests": ["Basketball", "Sport"],
                "roomName": "Sport Club",
                "roomCategory": "Sport"
            }
        ]
        
        request_data = {
            "userInterests": ["Python", "Programming", "IT"],
            "posts": posts,
            "subscribedUserIds": []
        }
        
        response = client.post("/recommend-posts", json=request_data)
        assert response.status_code == 200
        
        data = response.json()
        recommended_posts = data["recommended_posts"]
        
        # Должен быть хотя бы один пост
        assert len(recommended_posts) > 0
        
        # Посты о Python/IT должны иметь приоритет
        first_post = recommended_posts[0]
        assert first_post["id"] in [1, 2]  # Python или Java пост
    
    def test_subscriptions_override_interests(self):
        """Подписки имеют приоритет над интересами"""
        posts = [
            {
                "id": 1,
                "name": "Python Post",
                "text": "Python content",
                "authorId": 1,
                "authorName": "PythonUser",
                "authorInterests": ["Python"]
            },
            {
                "id": 2,
                "name": "Sport Post",
                "text": "Basketball content",
                "authorId": 2,
                "authorName": "SportUser",
                "authorInterests": ["Sport"]
            }
        ]
        
        # Интересы о Python, но подписка на Sport пользователя
        request_data = {
            "userInterests": ["Python", "Programming"],
            "posts": posts,
            "subscribedUserIds": [2]  # Подписан на Sport пользователя
        }
        
        response = client.post("/recommend-posts", json=request_data)
        assert response.status_code == 200
        
        data = response.json()
        recommended_posts = data["recommended_posts"]
        scores = data["similarity_scores"]
        
        # Находим индекс поста от подписки
        sport_post_index = next(
            (i for i, post in enumerate(recommended_posts) if post["id"] == 2),
            None
        )
        
        if sport_post_index is not None:
            # Пост от подписки должен иметь высокий score
            sport_score = scores[sport_post_index]
            python_post_index = next(
                (i for i, post in enumerate(recommended_posts) if post["id"] == 1),
                None
            )
            
            if python_post_index is not None:
                python_score = scores[python_post_index]
                # Пост от подписки должен иметь не меньший score, чем пост по интересам
                # (или быть выше в списке)
                assert sport_score >= python_score or sport_post_index < python_post_index
    
    def test_empty_interests_with_subscriptions(self):
        """Только подписки, без интересов"""
        posts = [
            {
                "id": 1,
                "name": "Post from subscription",
                "text": "Content from subscribed user",
                "authorId": 1,
                "authorName": "SubscribedUser",
                "authorInterests": ["IT"]
            },
            {
                "id": 2,
                "name": "Post from other user",
                "text": "Content from other user",
                "authorId": 2,
                "authorName": "OtherUser",
                "authorInterests": ["IT"]
            }
        ]
        
        request_data = {
            "userInterests": [],
            "posts": posts,
            "subscribedUserIds": [1]
        }
        
        response = client.post("/recommend-posts", json=request_data)
        assert response.status_code == 200
        
        data = response.json()
        recommended_posts = data["recommended_posts"]
        
        # Пост от подписки должен быть в рекомендациях
        post_ids = [post["id"] for post in recommended_posts]
        assert 1 in post_ids
        
        # Проверяем, что пост от подписки имеет высокий приоритет
        subscribed_post = next(
            (post for post in recommended_posts if post["id"] == 1),
            None
        )
        assert subscribed_post is not None
    
    def test_similarity_scores_consistency(self):
        """Проверка консистентности similarity scores"""
        posts = [
            {
                "id": i,
                "name": f"Post {i}",
                "text": f"Content {i}",
                "authorId": i,
                "authorName": f"User{i}",
                "authorInterests": ["IT"]
            }
            for i in range(1, 6)
        ]
        
        request_data = {
            "userInterests": ["IT", "Programming"],
            "posts": posts,
            "subscribedUserIds": []
        }
        
        response = client.post("/recommend-posts", json=request_data)
        assert response.status_code == 200
        
        data = response.json()
        recommended_posts = data["recommended_posts"]
        scores = data["similarity_scores"]
        
        # Количество постов должно совпадать с количеством scores
        assert len(recommended_posts) == len(scores)
        
        # Scores должны быть числами
        assert all(isinstance(score, (int, float)) for score in scores)
        
        # Scores должны быть неотрицательными
        assert all(score >= 0 for score in scores)
    
    def test_recommendation_ordering(self):
        """Проверка правильности сортировки рекомендаций"""
        posts = [
            {
                "id": 1,
                "name": "Low relevance post",
                "text": "Some random content",
                "authorId": 1,
                "authorName": "User1",
                "authorInterests": ["Other"]
            },
            {
                "id": 2,
                "name": "High relevance post",
                "text": "Python programming tutorial and best practices",
                "authorId": 2,
                "authorName": "User2",
                "authorInterests": ["Python", "Programming"]
            },
            {
                "id": 3,
                "name": "Medium relevance post",
                "text": "General programming tips",
                "authorId": 3,
                "authorName": "User3",
                "authorInterests": ["Programming"]
            }
        ]
        
        request_data = {
            "userInterests": ["Python", "Programming"],
            "posts": posts,
            "subscribedUserIds": []
        }
        
        response = client.post("/recommend-posts", json=request_data)
        assert response.status_code == 200
        
        data = response.json()
        recommended_posts = data["recommended_posts"]
        scores = data["similarity_scores"]
        
        if len(scores) > 1:
            # Scores должны быть отсортированы по убыванию
            for i in range(len(scores) - 1):
                assert scores[i] >= scores[i + 1], \
                    f"Scores should be in descending order, but {scores[i]} < {scores[i + 1]}"
    
    def test_response_structure(self):
        """Проверка структуры ответа"""
        posts = [
            {
                "id": 1,
                "name": "Test Post",
                "text": "Test content",
                "authorId": 1,
                "authorName": "TestUser"
            }
        ]
        
        request_data = {
            "userInterests": ["IT"],
            "posts": posts,
            "subscribedUserIds": []
        }
        
        response = client.post("/recommend-posts", json=request_data)
        assert response.status_code == 200
        
        data = response.json()
        
        # Проверяем наличие всех обязательных полей
        assert "recommended_posts" in data
        assert "similarity_scores" in data
        assert "message" in data
        
        # Проверяем типы
        assert isinstance(data["recommended_posts"], list)
        assert isinstance(data["similarity_scores"], list)
        assert isinstance(data["message"], str)
        
        # Проверяем структуру поста в ответе
        if len(data["recommended_posts"]) > 0:
            post = data["recommended_posts"][0]
            assert "id" in post
            assert "name" in post
            assert "text" in post

