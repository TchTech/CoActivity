from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.feature_extraction.text import TfidfVectorizer
import uvicorn

# Загрузка стоп-слов с правильной кодировкой
try:
    with open("russian.txt", "r", encoding="utf-8") as f:
        russian_stop_words = f.read().split()
except (FileNotFoundError, UnicodeDecodeError):
    try:
        with open("russian.txt", "r", encoding="cp1251") as f:
            russian_stop_words = f.read().split()
    except Exception:
        russian_stop_words = []


# Модели Pydantic
class Room(BaseModel):
    id: int
    name: str
    description: str
    interest_type: str
    geoposition: str
    sub_interest_type: str
    location: str


class UserInterests(BaseModel):
    interests: List[str]
    preferred_categories: Optional[List[str]] = None
    location: Optional[str] = None


class RecommendationRequest(BaseModel):
    user_interests: UserInterests
    rooms: List[Room]
    history_of_users_rooms: Optional[List[Room]] = None


class RecommendationResponse(BaseModel):
    recommended_rooms: List[Room]
    similarity_scores: List[float]
    message: str


# Модели для рекомендаций постов
class Post(BaseModel):
    id: int
    name: str
    text: str
    author_name: Optional[str] = Field(None, alias="authorName")  # Поддержка camelCase от Java
    author_id: Optional[int] = Field(None, alias="authorId")  # ID автора для проверки подписок
    author_interests: Optional[List[str]] = Field(None, alias="authorInterests")
    room_name: Optional[str] = Field(None, alias="roomName")
    room_category: Optional[str] = Field(None, alias="roomCategory")
    
    class Config:
        populate_by_name = True  # Разрешает использование обоих имен (snake_case и camelCase)


class PostRecommendationRequest(BaseModel):
    user_interests: Optional[List[str]] = Field(None, alias="userInterests")
    posts: List[Post]
    subscribed_user_ids: Optional[List[int]] = Field(None, alias="subscribedUserIds")
    
    class Config:
        populate_by_name = True


class PostRecommendationResponse(BaseModel):
    recommended_posts: List[Post]
    similarity_scores: List[float]
    message: str


# Инициализация FastAPI
app = FastAPI(
    title="Recommendation System API",
    description="API для рекомендации комнат на основе интересов пользователя",
    version="1.0.1"
)

# Пример данных о комнатах
sample_rooms = [
    Room(
        id=1,
        name="Изучение Python для начинающих",
        description="Основы программирования на Python для начинающих, простые проекты",
        interest_type="IT",
        sub_interest_type="Программирование",
        geoposition="Москва",
        location="Онлайн"
    ),
    Room(
        id=2,
        name="Продвинутый Python: асинхронное программирование",
        description="Изучаем асинхронность в Python, asyncio и продвинутые темы",
        interest_type="IT",
        sub_interest_type="Программирование",
        geoposition="Москва",
        location="Онлайн"
    ),
    Room(
        id=3,
        name="Data Science с Python и Pandas",
        description="Анализ данных с помощью Python, библиотеки Pandas и NumPy",
        interest_type="Science",
        sub_interest_type="Data Science",
        geoposition="Москва",
        location="Онлайн"
    ),
    Room(
        id=4,
        name="Программирование на C++ с нуля",
        description="Изучение C++ с основ: указатели, классы, ООП",
        interest_type="IT",
        sub_interest_type="Программирование",
        geoposition="Москва",
        location="Онлайн"
    ),
    Room(
        id=5,
        name="Футбол для любителей",
        description="Регулярные футбольные матчи для любителей любого уровня",
        interest_type="Sport",
        sub_interest_type="Футбол",
        geoposition="Москва",
        location="Москва"
    ),
    Room(
        id=6,
        name="Баскетбол для новичков",
        description="Обучение баскетболу с основными правилами и техникой",
        interest_type="Sport",
        sub_interest_type="Баскетбол",
        geoposition="Москва",
        location="Москва"
    ),
    Room(
        id=7,
        name="Йога и медитация",
        description="Йога для расслабления и медитация для снятия стресса",
        interest_type="LifeStyle",
        sub_interest_type="Йога",
        geoposition="Москва",
        location="Онлайн"
    ),
    Room(
        id=8,
        name="Веб-разработка на Django",
        description="Создание веб-приложений на Django framework",
        interest_type="IT",
        sub_interest_type="Веб-разработка",
        geoposition="Москва",
        location="Онлайн"
    ),
    Room(
        id=9,
        name="Уличный баскетбол 3x3",
        description="Уличный баскетбол в формате 3 на 3, турниры",
        interest_type="Sport",
        sub_interest_type="Баскетбол",
        geoposition="Москва",
        location="Москва"
    ),
    Room(
        id=10,
        name="Java для enterprise приложений",
        description="Корпоративные приложения на Java Spring Boot",
        interest_type="IT",
        sub_interest_type="Программирование",
        geoposition="Москва",
        location="Онлайн"
    )
]


class RecommendationService:
    def __init__(self):
        valid_stop_words = [word for word in russian_stop_words if word and len(word.strip()) > 0]
        self.tfidf_vectorizer = TfidfVectorizer(
            stop_words=valid_stop_words if valid_stop_words else None,
            ngram_range=(1, 2),
            min_df=1,
            max_features=100
        )
        # Отдельный векторizer для постов
        self.post_tfidf_vectorizer = TfidfVectorizer(
            stop_words=valid_stop_words if valid_stop_words else None,
            ngram_range=(1, 2),
            min_df=1,
            max_features=100
        )

    def prepare_text_data(self, rooms: List[Room]) -> List[str]:
        """Подготавливает текстовые данные из комнат для TF-IDF"""
        texts = []
        for room in rooms:
            text = f"{room.name} {room.description} {room.interest_type} {room.sub_interest_type} {room.geoposition}"
            texts.append(text)
        return texts

    def get_scores(self, user_interests: List[str], history_of_users_rooms: List[Room], rooms: List[Room]) -> dict:
        """Получает оценки схожести на основе интересов пользователя и истории"""
        if not rooms:
            return {}
        
        # Подготавливаем тексты для всех комнат
        room_texts = self.prepare_text_data(rooms)
        
        # Создаем векторное представление комнат
        room_vectors = self.tfidf_vectorizer.fit_transform(room_texts)
        
        # Подготавливаем текст пользователя (интересы + история)
        user_text = " ".join(user_interests)
        if history_of_users_rooms:
            history_texts = self.prepare_text_data(history_of_users_rooms)
            user_text += " " + " ".join(history_texts)
        
        user_vector = self.tfidf_vectorizer.transform([user_text])
        
        similarities = cosine_similarity(user_vector, room_vectors)[0]
        
        room_scores = {}
        for i, room in enumerate(rooms):
            room_scores[room.id] = float(similarities[i])
        
        return room_scores

    def filter_recommendations(self, recommendations: List[tuple],
                               preferred_categories: Optional[List[str]] = None,
                               geoposition: Optional[str] = None) -> List[tuple]:
        """Фильтрует рекомендации по категориям и местоположению"""
        filtered = []
        for room, score in recommendations:
            include = True
            if preferred_categories and room.interest_type not in preferred_categories:
                include = False
            if geoposition and room.geoposition != geoposition:
                include = False
            if include:
                filtered.append((room, score))
        return filtered

    def first_step_recomendation(self, history_of_users_rooms: List[Room], user_interests: List[str]) -> dict:
        """Вычисляет веса категорий на основе истории и интересов пользователя"""
        counter_dict = {
            "IT": 0, "Science": 0, "Business": 0, "Sport": 0,
            "Art": 0, "Entertainment": 0, "LifeStyle": 0, "Other": 0
        }
        for category in counter_dict.keys():
            if category in user_interests:
                counter_dict[category] = 1 / (len(user_interests) * 2) if len(user_interests) > 0 else 0
        n = 0
        for room in history_of_users_rooms:
            if room.interest_type in counter_dict:
                counter_dict[room.interest_type] += 1
                n += 1
        scores_dict = {}
        for key in counter_dict:
            if n > 0:
                scores_dict[key] = counter_dict[key] / (n * 2)
            else:
                scores_dict[key] = counter_dict[key]
        
        return scores_dict

    def completed_recommendation(self, history_of_users_rooms: List[Room], 
                                 actual_rooms: List[Room], 
                                 user_interests: List[str]) -> List[tuple]:
        """Получает финальные рекомендации с учетом категорий и TF-IDF"""
        interests_scores_dict = self.first_step_recomendation(history_of_users_rooms, user_interests)
        rooms_scores_by_id = self.get_scores(user_interests, history_of_users_rooms, actual_rooms)
        for room in actual_rooms:
            category_score = interests_scores_dict.get(room.interest_type, 0)
            tfidf_score = rooms_scores_by_id.get(room.id, 0)
            rooms_scores_by_id[room.id] = tfidf_score * (1 + category_score)
        result = [(room, rooms_scores_by_id.get(room.id, 0)) for room in actual_rooms]
        result.sort(key=lambda x: x[1], reverse=True)
        
        return result

    def prepare_post_text_data(self, posts: List[Post]) -> List[str]:
        """Подготавливает текстовые данные из постов для TF-IDF"""
        texts = []
        for post in posts:
            # Собираем текст из названия, содержимого поста, интересов автора и категории комнаты
            text_parts = [post.name or "", post.text or ""]
            if post.author_interests:
                text_parts.extend(post.author_interests)
            if post.room_category:
                text_parts.append(post.room_category)
            if post.room_name:
                text_parts.append(post.room_name)
            text = " ".join(text_parts)
            texts.append(text)
        return texts

    def recommend_posts(self, user_interests: List[str], posts: List[Post], 
                       subscribed_user_ids: Optional[List[int]] = None) -> List[tuple]:
        """Рекомендует посты на основе интересов пользователя или подписок"""
        if not posts:
            return []
        
        subscribed_ids_set = set(subscribed_user_ids) if subscribed_user_ids else set()
        has_subscriptions = len(subscribed_ids_set) > 0
        
        # Если есть подписки - используем логику на основе подписок
        if has_subscriptions:
            # Создаем профиль пользователя из интересов и постов от подписок
            subscribed_posts = [p for p in posts if p.author_id and p.author_id in subscribed_ids_set]
            
            # Подготавливаем тексты для всех постов
            post_texts = self.prepare_post_text_data(posts)
            post_vectors = self.post_tfidf_vectorizer.fit_transform(post_texts)
            
            # Формируем профиль пользователя: интересы + содержимое постов от подписок
            user_profile_parts = []
            if user_interests:
                user_profile_parts.extend(user_interests)
            
            # Добавляем содержимое постов от подписок в профиль
            for post in subscribed_posts:
                post_parts = [post.name or "", post.text or ""]
                if post.author_interests:
                    post_parts.extend(post.author_interests)
                if post.room_category:
                    post_parts.append(post.room_category)
                user_profile_parts.extend(post_parts)
            
            if not user_profile_parts:
                # Если нет интересов и постов от подписок, используем только посты от подписок
                # Приоритизируем их выше остальных
                post_scores = []
                for post in posts:
                    if post.author_id and post.author_id in subscribed_ids_set:
                        post_scores.append((post, 2.0))  # Высокий приоритет для постов от подписок
                    else:
                        post_scores.append((post, 0.5))  # Низкий приоритет для остальных
                post_scores.sort(key=lambda x: x[1], reverse=True)
                return post_scores
            
            user_text = " ".join(user_profile_parts)
            user_vector = self.post_tfidf_vectorizer.transform([user_text])
            similarities = cosine_similarity(user_vector, post_vectors)[0]
            
            # Увеличиваем вес постов от подписок
            post_scores = []
            for i, post in enumerate(posts):
                base_score = float(similarities[i])
                
                # Если это пост от подписки - значительно увеличиваем вес
                if post.author_id and post.author_id in subscribed_ids_set:
                    final_score = base_score * 2.0 + 1.0  # Значительное увеличение веса
                else:
                    final_score = base_score
                
                post_scores.append((post, final_score))
        
        else:
            # Если нет подписок - используем только интересы пользователя
            if not user_interests:
                # Если нет интересов, возвращаем все посты с одинаковым весом
                return [(post, 1.0) for post in posts]
            
            # Подготавливаем тексты для всех постов
            post_texts = self.prepare_post_text_data(posts)
            post_vectors = self.post_tfidf_vectorizer.fit_transform(post_texts)
            
            # Формируем профиль пользователя только из интересов
            user_text = " ".join(user_interests)
            user_vector = self.post_tfidf_vectorizer.transform([user_text])
            similarities = cosine_similarity(user_vector, post_vectors)[0]
            
            post_scores = []
            for i, post in enumerate(posts):
                base_score = float(similarities[i])
                post_scores.append((post, base_score))
        
        # Сортируем по убыванию оценки
        post_scores.sort(key=lambda x: x[1], reverse=True)
        
        return post_scores


recommendation_service = RecommendationService()


@app.get("/")
async def root():
    return {
        "message": "Recommendation System API",
        "version": "1.0.1",
        "endpoints": {
            "recommend": "/recommend - POST - получить рекомендации",
            "sample-rooms": "/sample-rooms - GET - получить пример комнат",
            "health": "/health - GET - проверка здоровья API"
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
        history_of_users_rooms = request.history_of_users_rooms or []

        if not user_interests:
            raise HTTPException(status_code=400, detail="Интересы пользователя не могут быть пустыми")

        if not rooms:
            raise HTTPException(status_code=400, detail="Список комнат не может быть пустым")

        recommendations = recommendation_service.completed_recommendation(
            history_of_users_rooms=history_of_users_rooms,
            actual_rooms=rooms,
            user_interests=user_interests
        )

        if request.user_interests.preferred_categories or request.user_interests.location:
            recommendations = recommendation_service.filter_recommendations(
                recommendations=recommendations,
                preferred_categories=request.user_interests.preferred_categories,
                geoposition=request.user_interests.location
            )

        recommendations = recommendations[:5]

        if not recommendations:
            return RecommendationResponse(
                recommended_rooms=[],
                similarity_scores=[],
                message="Не найдено подходящих рекомендаций по заданным критериям"
            )

        recommended_rooms = [rec[0] for rec in recommendations]
        similarity_scores = [float(rec[1]) for rec in recommendations]

        return RecommendationResponse(
            recommended_rooms=recommended_rooms,
            similarity_scores=similarity_scores,
            message=f"Найдено {len(recommended_rooms)} рекомендаций"
        )

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Ошибка при получении рекомендаций: {str(e)}")


@app.post("/recommend-from-sample", response_model=RecommendationResponse)
async def get_recommendations_from_sample(user_interests: UserInterests):
    """Получает рекомендации из примеров комнат"""
    request = RecommendationRequest(
        user_interests=user_interests,
        rooms=sample_rooms,
        history_of_users_rooms=None
    )
    return await get_recommendations(request)


@app.post("/recommend-posts", response_model=PostRecommendationResponse)
async def get_post_recommendations(request: PostRecommendationRequest):
    """Получает рекомендации постов на основе интересов пользователя или подписок"""
    try:
        # Pydantic автоматически заполняет поля из alias, если они были переданы
        user_interests = request.user_interests or []
        posts = request.posts
        subscribed_user_ids = request.subscribed_user_ids or []

        if not posts:
            return PostRecommendationResponse(
                recommended_posts=[],
                similarity_scores=[],
                message="Нет постов для рекомендации"
            )

        # Если нет интересов и подписок, возвращаем все посты
        if not user_interests and not subscribed_user_ids:
            return PostRecommendationResponse(
                recommended_posts=posts[:50],  # Ограничиваем до 50 постов
                similarity_scores=[1.0] * min(len(posts), 50),
                message=f"Возвращено {min(len(posts), 50)} постов (нет интересов и подписок)"
            )

        recommendations = recommendation_service.recommend_posts(
            user_interests=user_interests,
            posts=posts,
            subscribed_user_ids=subscribed_user_ids
        )

        # Ограничиваем количество рекомендаций (до 50)
        recommendations = recommendations[:50]

        if not recommendations:
            return PostRecommendationResponse(
                recommended_posts=[],
                similarity_scores=[],
                message="Не найдено подходящих постов"
            )

        recommended_posts = [rec[0] for rec in recommendations]
        similarity_scores = [float(rec[1]) for rec in recommendations]

        return PostRecommendationResponse(
            recommended_posts=recommended_posts,
            similarity_scores=similarity_scores,
            message=f"Найдено {len(recommended_posts)} рекомендаций"
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Ошибка при получении рекомендаций постов: {str(e)}")


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
