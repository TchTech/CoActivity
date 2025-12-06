-- Создание пользователя и базы данных для CoActivity
DROP USER IF EXISTS "Gr1zBear";
CREATE USER "Gr1zBear" WITH PASSWORD 'qwerty';
DROP DATABASE IF EXISTS "CoPos";
CREATE DATABASE "CoPos" OWNER "Gr1zBear";
GRANT ALL PRIVILEGES ON DATABASE "CoPos" TO "Gr1zBear";

