# Инструкция по настройке PostgreSQL базы данных

## Проблема
Ошибка аутентификации при подключении к PostgreSQL (SQLState: 28P01).

## Решение

### Вариант 1: Использование pgAdmin (GUI)

1. Откройте **pgAdmin 4**
2. Подключитесь к серверу PostgreSQL (используя пароль суперпользователя)
3. Откройте **Query Tool** (SQL Editor)
4. Выполните следующий SQL:

```sql
-- Удаляем пользователя, если существует
DROP USER IF EXISTS "Gr1zBear";

-- Создаем пользователя с паролем
CREATE USER "Gr1zBear" WITH PASSWORD 'qwerty';

-- Создаем базу данных
CREATE DATABASE "CoPos" OWNER "Gr1zBear";

-- Подключаемся к новой базе
\c "CoPos"

-- Выдаем привилегии
GRANT ALL ON SCHEMA public TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO "Gr1zBear";
```

### Вариант 2: Использование командной строки PowerShell

1. Откройте PowerShell **от имени администратора**
2. Добавьте PostgreSQL в PATH:
   ```powershell
   $env:Path += ";C:\Program Files\PostgreSQL\18\bin"
   ```
   
3. Установите пароль для пользователя postgres:
   ```powershell
   $env:PGPASSWORD = "ваш_пароль_postgres"
   ```
   
   Если вы не знаете пароль postgres, попробуйте:
   - Пустую строку
   - `postgres`
   - Ваш системный пароль Windows

4. Выполните SQL скрипт:
   ```powershell
   psql -U postgres -f create_user_and_db.sql
   ```

   Или выполните команды напрямую:
   ```powershell
   psql -U postgres -c "DROP USER IF EXISTS `"Gr1zBear`";"
   psql -U postgres -c "CREATE USER `"Gr1zBear`" WITH PASSWORD 'qwerty';"
   psql -U postgres -c "CREATE DATABASE `"CoPos`" OWNER `"Gr1zBear`";"
   ```

### Вариант 3: Использование SQL файла

1. Скопируйте содержимое файла `create_user_and_db.sql`
2. Откройте любой PostgreSQL клиент (pgAdmin, DBeaver, DataGrip и т.д.)
3. Подключитесь к PostgreSQL как суперпользователь
4. Выполните SQL скрипт

### Проверка

После создания базы данных и пользователя, проверьте подключение:

```powershell
$env:PGPASSWORD = "qwerty"
psql -h localhost -p 5432 -U Gr1zBear -d CoPos -c "SELECT version();"
```

Если подключение успешно, вы увидите версию PostgreSQL.

### Если проблема сохраняется

1. **Проверьте, запущен ли PostgreSQL:**
   ```powershell
   Get-Service -Name "*postgres*"
   ```

2. **Проверьте порт:**
   ```powershell
   netstat -ano | findstr ":5432"
   ```

3. **Проверьте настройки аутентификации в `pg_hba.conf`:**
   - Обычно находится в `C:\Program Files\PostgreSQL\18\data\pg_hba.conf`
   - Убедитесь, что для localhost используется метод `trust` или `md5`

4. **Измените метод аутентификации на trust (временно для теста):**
   - Откройте `pg_hba.conf`
   - Найдите строку: `host all all 127.0.0.1/32 scram-sha-256`
   - Замените на: `host all all 127.0.0.1/32 trust`
   - Перезапустите PostgreSQL:
     ```powershell
     Restart-Service postgresql*
     ```

## Параметры подключения

После настройки используйте следующие параметры в `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/CoPos
spring.datasource.username=Gr1zBear
spring.datasource.password=qwerty
spring.datasource.driver-class-name=org.postgresql.Driver
```

## Быстрая проверка

После настройки попробуйте запустить Spring Boot приложение снова:

```bash
mvn spring-boot:run
```

Если ошибка исчезла - настройка успешна!

