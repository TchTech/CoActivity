========================================
РЕШЕНИЕ ПРОБЛЕМЫ С БАЗОЙ ДАННЫХ
========================================

ПРОБЛЕМА:
Ошибка аутентификации PostgreSQL - пользователь "Gr1zBear" не существует.

РЕШЕНИЕ (выберите один вариант):

--- ВАРИАНТ 1: ЧЕРЕЗ PGADMIN (САМЫЙ ПРОСТОЙ) ---

1. Откройте pgAdmin 4
2. Подключитесь к PostgreSQL серверу (обычно пароль: postgres)
3. Правой кнопкой на "PostgreSQL" -> Query Tool
4. Выполните следующие SQL команды:

   CREATE USER "Gr1zBear" WITH PASSWORD 'qwerty';
   CREATE DATABASE "CoPos" OWNER "Gr1zBear";
   GRANT ALL PRIVILEGES ON DATABASE "CoPos" TO "Gr1zBear";

5. Перезапустите Spring Boot приложение

--- ВАРИАНТ 2: ЧЕРЕЗ КОМАНДНУЮ СТРОКУ ---

Откройте PowerShell от имени администратора и выполните:

$env:Path += ";C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = "ваш_пароль_postgres"  # Замените на ваш пароль!
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE USER `"Gr1zBear`" WITH PASSWORD 'qwerty';"
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE DATABASE `"CoPos`" OWNER `"Gr1zBear`";"
psql -h localhost -p 5432 -U postgres -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE `"CoPos`" TO `"Gr1zBear`";"

Или используйте файл create_db.sql:

$env:Path += ";C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = "ваш_пароль_postgres"
psql -h localhost -p 5432 -U postgres -d postgres -f create_db.sql

--- ВАРИАНТ 3: ЧЕРЕЗ БАТНИК ---

Просто запустите FIX_DATABASE.bat двойным кликом
и введите пароль postgres когда он попросит.

--- ЕСЛИ НЕ ЗНАЕТЕ ПАРОЛЬ POSTGRES ---

Обычные варианты пароля:
- postgres
- ваш_пароль_windows
- пустая строка (просто Enter)

Если ничего не помогает:
1. Откройте pgAdmin
2. Правой кнопкой на сервер PostgreSQL -> Properties
3. Перейдите на вкладку Connection
4. Измените пароль или используйте его для подключения

После создания пользователя и БД приложение должно запуститься!

