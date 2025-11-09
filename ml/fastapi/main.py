from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import numpy as np
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.feature_extraction.text import TfidfVectorizer
import uvicorn

# Русские стоп-слова
russian_stop_words = [
    'и', 'в', 'во', 'не', 'что', 'он', 'на', 'я', 'с', 'со', 'как', 'а', 'то', 'все', 'она', 'так', 'его',
    'но', 'да', 'ты', 'к', 'у', 'же', 'вы', 'за', 'бы', 'по', 'только', 'ее', 'мне', 'было', 'вот', 'от',
    'меня', 'еще', 'нет', 'о', 'из', 'ему', 'теперь', 'когда', 'даже', 'ну', 'вдруг', 'ли', 'если', 'уже',
    'или', 'ни', 'быть', 'был', 'него', 'до', 'вас', 'нибудь', 'опять', 'уж', 'вам', 'ведь', 'там', 'потом',
    'себя', 'ничего', 'ей', 'может', 'они', 'тут', 'где', 'есть', 'надо', 'ней', 'для', 'мы', 'тебя', 'их',
    'чем', 'была', 'сам', 'чтоб', 'без', 'будто', 'чего', 'раз', 'тоже', 'себе', 'под', 'будет', 'ж', 'тогда',
    'кто', 'этот', 'того', 'потому', 'этого', 'какой', 'совсем', 'ним', 'здесь', 'этом', 'один', 'почти',
    'мой', 'тем', 'чтобы', 'нее', 'сейчас', 'были', 'куда', 'зачем', 'всех', 'никогда', 'можно', 'при',
    'наконец', 'два', 'об', 'другой', 'хоть', 'после', 'над', 'больше', 'тот', 'через', 'эти', 'нас', 'про',
    'всего', 'них', 'какая', 'много', 'разве', 'три', 'эту', 'моя', 'впрочем', 'хорошо', 'свою', 'этой',
    'перед', 'иногда', 'лучше', 'чуть', 'том', 'нельзя', 'такой', 'им', 'более', 'всегда', 'конечно', 'всю',
    'между'
]


# Модели Pydantic
class Room(BaseModel):
    id: int
    title: str
    description: str
    category: str
    subcategory: str
    location: str


class UserInterests(BaseModel):
    interests: List[str]
    preferred_categories: Optional[List[str]] = None
    location: Optional[str] = None


class RecommendationRequest(BaseModel):
    user_interests: UserInterests
    rooms: List[Room]


class RecommendationResponse(BaseModel):
    recommended_rooms: List[Room]
    similarity_scores: List[float]
    message: str


# Инициализация FastAPI
app = FastAPI(
    title="Recommendation System API",
    description="API для рекомендации комнат на основе интересов пользователя",
    version="1.0.0"
)

# Пример данных о комнатах
sample_rooms = [
    Room(
        id=1,
        title="Изучение Python для начинающих",
        description="Основы программирования на Python для начинающих, простые проекты",
        category="Программирование",
        subcategory="Python",
        location="Онлайн"
    ),
    Room(
        id=2,
        title="Продвинутый Python: асинхронное программирование",
        description="Изучаем асинхронность в Python, asyncio и продвинутые темы",
        category="Программирование",
        subcategory="Python",
        location="Онлайн"
    ),
    Room(
        id=3,
        title="Data Science с Python и Pandas",
        description="Анализ данных с помощью Python, библиотеки Pandas и NumPy",
        category="Программирование",
        subcategory="Python",
        location="Онлайн"
    ),
    Room(
        id=4,
        title="Программирование на C++ с нуля",
        description="Изучение C++ с основ: указатели, классы, ООП",
        category="Программирование",
        subcategory="C++",
        location="Онлайн"
    ),
    Room(
        id=5,
        title="Футбол для любителей",
        description="Регулярные футбольные матчи для любителей любого уровня",
        category="Спорт",
        subcategory="Футбол",
        location="Москва"
    ),
    Room(
        id=6,
        title="Баскетбол для новичков",
        description="Обучение баскетболу с основными правилами и техникой",
        category="Спорт",
        subcategory="Баскетбол",
        location="Москва"
    ),
    Room(
        id=7,
        title="Йога и медитация",
        description="Йога для расслабления и медитация для снятия стресса",
        category="Спорт",
        subcategory="Йога",
        location="Онлайн"
    ),
    Room(
        id=8,
        title="Веб-разработка на Django",
        description="Создание веб-приложений на Django framework",
        category="Программирование",
        subcategory="Python",
        location="Онлайн"
    ),
    Room(
        id=9,
        title="Уличный баскетбол 3x3",
        description="Уличный баскетбол в формате 3 на 3, турниры",
        category="Спорт",
        subcategory="Баскетбол",
        location="Москва"
    ),
    Room(
        id=10,
        title="Java для enterprise приложений",
        description="Корпоративные приложения на Java Spring Boot",
        category="Программирование",
        subcategory="Java",
        location="Онлайн"
    )
]


class RecommendationService:
    def __init__(self):
        self.tfidf_vectorizer = TfidfVectorizer(
            stop_words=russian_stop_words,
            ngram_range=(1, 2),
            min_df=1,
            max_features=100
        )

    def prepare_text_data(self, rooms: List[Room]) -> List[str]:
        """Подготавливает текстовые данные из комнат для TF-IDF"""
        texts = []
        for room in rooms:
            # Комбинируем название, описание и категорию для лучшего представления
            text = f"{room.title} {room.description} {room.category} {room.subcategory}"
            texts.append(text)
        return texts

    def get_recommendations(self, user_interests: List[str], rooms: List[Room], top_n: int = 5) -> List[tuple]:
        """Получает рекомендации на основе интересов пользователя"""
        if not rooms:
            return []

        # Подготавливаем текстовые данные
        room_texts = self.prepare_text_data(rooms)

        # Обучаем TF-IDF на данных комнат
        room_vectors = self.tfidf_vectorizer.fit_transform(room_texts)

        # Создаем текстовое представление интересов пользователя
        user_text = " ".join(user_interests)
        user_vector = self.tfidf_vectorizer.transform([user_text])

        # Вычисляем косинусное сходство
        similarities = cosine_similarity(user_vector, room_vectors)[0]

        # Сортируем комнаты по сходству
        room_scores = list(zip(rooms, similarities))
        room_scores.sort(key=lambda x: x[1], reverse=True)

        # Возвращаем топ-N рекомендаций
        return room_scores[:top_n]

    def filter_recommendations(self, recommendations: List[tuple],
                               preferred_categories: Optional[List[str]] = None,
                               location: Optional[str] = None) -> List[tuple]:
        """Фильтрует рекомендации по категориям и местоположению"""
        filtered = []

        for room, score in recommendations:
            include = True

            # Фильтр по категориям
            if preferred_categories and room.category not in preferred_categories:
                include = False

            # Фильтр по местоположению
            if location and room.location != location:
                include = False

            if include:
                filtered.append((room, score))

        return filtered


# Инициализация сервиса рекомендаций
recommendation_service = RecommendationService()


@app.get("/")
async def root():
    return {
        "message": "Recommendation System API",
        "version": "1.0.0",
        "endpoints": {
            "recommend": "/recommend - POST - получить рекомендации",
            "sample_rooms": "/sample-rooms - GET - получить пример комнат"
        }
    }


@app.get("/sample-rooms", response_model=List[Room])
async def get_sample_rooms():
    """Возвращает список примеров комнат"""
    return sample_rooms


@app.post("/recommend", response_model=RecommendationResponse)
async def get_recommendations(request: RecommendationRequest):
    """Получает рекомендации комнат на основе интересов пользователя"""
    try:
        user_interests = request.user_interests.interests
        rooms = request.rooms

        if not user_interests:
            raise HTTPException(status_code=400, detail="Интересы пользователя не могут быть пустыми")

        if not rooms:
            raise HTTPException(status_code=400, detail="Список комнат не может быть пустым")

        # Получаем рекомендации
        recommendations = recommendation_service.get_recommendations(
            user_interests=user_interests,
            rooms=rooms,
            top_n=10  # Получаем больше рекомендаций для последующей фильтрации
        )

        # Применяем фильтры если указаны
        if request.user_interests.preferred_categories or request.user_interests.location:
            recommendations = recommendation_service.filter_recommendations(
                recommendations=recommendations,
                preferred_categories=request.user_interests.preferred_categories,
                location=request.user_interests.location
            )

        # Берем топ-5 после фильтрации
        recommendations = recommendations[:5]

        if not recommendations:
            return RecommendationResponse(
                recommended_rooms=[],
                similarity_scores=[],
                message="Не найдено подходящих рекомендаций по заданным критериям"
            )

        # Разделяем комнаты и scores
        recommended_rooms = [rec[0] for rec in recommendations]
        similarity_scores = [float(rec[1]) for rec in recommendations]

        return RecommendationResponse(
            recommended_rooms=recommended_rooms,
            similarity_scores=similarity_scores,
            message=f"Найдено {len(recommended_rooms)} рекомендаций"
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Ошибка при получении рекомендаций: {str(e)}")


@app.post("/recommend-from-sample", response_model=RecommendationResponse)
async def get_recommendations_from_sample(user_interests: UserInterests):
    """Получает рекомендации из примеров комнат"""
    return await get_recommendations(RecommendationRequest(
        user_interests=user_interests,
        rooms=sample_rooms
    ))


@app.get("/health")
async def health_check():
    """Проверка здоровья API"""
    return {"status": "healthy", "service": "recommendation_api"}


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )