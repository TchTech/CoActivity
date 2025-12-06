-- Скрипт для очистки всех данных из базы данных CoPos
-- Внимание: Этот скрипт удалит ВСЕ данные из всех таблиц!
-- Резервная копия данных не создается автоматически.

-- Отключаем проверку внешних ключей для безопасной очистки
SET session_replication_role = 'replica';

-- Удаляем данные из всех таблиц в правильном порядке (от зависимых к независимым)

-- Сначала очищаем таблицы с внешними ключами (зависимые таблицы)

-- Уведомления и связанные данные
TRUNCATE TABLE notification_deduplication_log CASCADE;
TRUNCATE TABLE notifications CASCADE;

-- История и логи
TRUNCATE TABLE room_join_request_history CASCADE;

-- Заявки на участие
TRUNCATE TABLE room_join_requests CASCADE;

-- Пин постов к комнатам
TRUNCATE TABLE room_post_pin CASCADE;

-- Сообщения в комнатах
TRUNCATE TABLE messages CASCADE;

-- Рейтинги пользователей
TRUNCATE TABLE user_ratings CASCADE;

-- Лайки и дизлайки на посты
TRUNCATE TABLE "likesOnPosts" CASCADE;
TRUNCATE TABLE "dislikesOnPosts" CASCADE;

-- Лайки и дизлайки на комментарии
TRUNCATE TABLE "likesOnComments" CASCADE;
TRUNCATE TABLE "dislikesOnComments" CASCADE;

-- Комментарии
TRUNCATE TABLE comments CASCADE;

-- Посты
TRUNCATE TABLE posts CASCADE;

-- Связи многие-ко-многим
TRUNCATE TABLE user_subscriptions CASCADE;
TRUNCATE TABLE user_interests CASCADE;
TRUNCATE TABLE room_admins CASCADE;
TRUNCATE TABLE "roomsCollaborators" CASCADE;

-- Настройки уведомлений
TRUNCATE TABLE room_notification_settings CASCADE;

-- Настройки пользователей
TRUNCATE TABLE user_settings CASCADE;

-- Отзывы
TRUNCATE TABLE feedbacks CASCADE;

-- Внешние ссылки
TRUNCATE TABLE external_links CASCADE;

-- Категории и интересы
TRUNCATE TABLE interests CASCADE;
TRUNCATE TABLE interest_categories CASCADE;

-- Комнаты
TRUNCATE TABLE rooms CASCADE;

-- Изображения
TRUNCATE TABLE images CASCADE;

-- Токены и верификация
TRUNCATE TABLE email_verification_tokens CASCADE;
TRUNCATE TABLE password_reset_tokens CASCADE;

-- Пользователи (очищаем последними, так как от них зависят другие таблицы)
TRUNCATE TABLE users CASCADE;

-- Включаем обратно проверку внешних ключей
SET session_replication_role = 'origin';

-- Сбрасываем последовательности (auto-increment счетчики)
ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS rooms_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS posts_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS comments_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS messages_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS images_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS user_ratings_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS room_join_requests_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS notifications_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS interests_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS interest_categories_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS external_links_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS password_reset_tokens_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS email_verification_tokens_id_seq RESTART WITH 1;

-- Сообщение об успешной очистке
SELECT 'База данных успешно очищена!' AS message;

