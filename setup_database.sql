-- Скрипт для создания базы данных и пользователя PostgreSQL
-- Запустите этот скрипт от имени суперпользователя PostgreSQL (обычно postgres)

-- Проверка и создание пользователя (если не существует)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'Gr1zBear') THEN
        CREATE USER "Gr1zBear" WITH PASSWORD 'qwerty';
        RAISE NOTICE 'User Gr1zBear created';
    ELSE
        RAISE NOTICE 'User Gr1zBear already exists';
        -- Обновляем пароль на всякий случай
        ALTER USER "Gr1zBear" WITH PASSWORD 'qwerty';
        RAISE NOTICE 'Password updated for Gr1zBear';
    END IF;
END
$$;

-- Проверка и создание базы данных (если не существует)
SELECT 'CREATE DATABASE "CoPos" OWNER "Gr1zBear"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'CoPos')\gexec

-- Выдача всех привилегий пользователю на базу данных
GRANT ALL PRIVILEGES ON DATABASE "CoPos" TO "Gr1zBear";

-- Подключение к базе данных для выдачи привилегий на схему
\c "CoPos"

-- Выдача привилегий на схему public
GRANT ALL ON SCHEMA public TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO "Gr1zBear";

\q

