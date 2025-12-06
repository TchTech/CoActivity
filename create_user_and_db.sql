-- Простой SQL скрипт для создания пользователя и базы данных
-- Запустите этот скрипт, подключившись к PostgreSQL как суперпользователь:
-- psql -U postgres -f create_user_and_db.sql

-- Удаляем пользователя, если существует (для чистого создания)
DROP USER IF EXISTS "Gr1zBear";

-- Создаем пользователя с паролем
CREATE USER "Gr1zBear" WITH PASSWORD 'qwerty';

-- Удаляем базу данных, если существует (опционально, закомментируйте если нужны данные)
-- DROP DATABASE IF EXISTS "CoPos";

-- Создаем базу данных
CREATE DATABASE "CoPos" OWNER "Gr1zBear";

-- Подключаемся к новой базе данных
\c "CoPos"

-- Выдаем все привилегии на схему public
GRANT ALL ON SCHEMA public TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO "Gr1zBear";

-- Проверка
\du "Gr1zBear"
\l "CoPos"

