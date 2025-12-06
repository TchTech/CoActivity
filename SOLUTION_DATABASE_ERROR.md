# 🔧 РЕШЕНИЕ ОШИБКИ: Пользователь Gr1zBear не существует

## Проблема
```
SQL Error: 0, SQLState: 28P01
Ошибка аутентификации: пользователь "Gr1zBear" не существует
```

## ✅ РЕШЕНИЕ (выберите один способ)

### Способ 1: Через pgAdmin (РЕКОМЕНДУЕТСЯ - самый простой)

1. **Откройте pgAdmin 4** (если установлен)
2. **Подключитесь к PostgreSQL серверу:**
   - Обычно пароль: `postgres` или ваш пароль Windows
   - Если не знаете пароль - см. "Если не знаете пароль" ниже
3. **Откройте Query Tool:**
   - Правой кнопкой на "PostgreSQL" → Query Tool
4. **Выполните SQL команды:**

```sql
CREATE USER "Gr1zBear" WITH PASSWORD 'qwerty';
CREATE DATABASE "CoPos" OWNER "Gr1zBear";
GRANT ALL PRIVILEGES ON DATABASE "CoPos" TO "Gr1zBear";
```

5. **Перезапустите Spring Boot приложение** - ошибка должна исчезнуть!

---

### Способ 2: Через командную строку PowerShell

Откройте PowerShell и выполните (замените `ВАШ_ПАРОЛЬ_POSTGRES` на реальный пароль):

```powershell
# Добавляем PostgreSQL в PATH
$env:Path += ";C:\Program Files\PostgreSQL\18\bin"

# Устанавливаем пароль postgres
$env:PGPASSWORD = "ВАШ_ПАРОЛЬ_POSTGRES"  # Замените на ваш пароль!

# Создаем пользователя
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE USER `"Gr1zBear`" WITH PASSWORD 'qwerty';"

# Создаем базу данных
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE DATABASE `"CoPos`" OWNER `"Gr1zBear`";"

# Выдаем привилегии
psql -h localhost -p 5432 -U postgres -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE `"CoPos`" TO `"Gr1zBear`";"
```

---

### Способ 3: Использовать готовый SQL файл

1. Откройте файл `create_db.sql` в корне проекта
2. Скопируйте содержимое
3. Выполните в pgAdmin Query Tool или через psql:

```powershell
$env:Path += ";C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = "ВАШ_ПАРОЛЬ_POSTGRES"
psql -h localhost -p 5432 -U postgres -d postgres -f create_db.sql
```

---

### Способ 4: Запустить батник FIX_DATABASE.bat

1. Найдите файл `FIX_DATABASE.bat` в корне проекта
2. Двойной клик для запуска
3. Введите пароль postgres, когда он попросит
4. Скрипт автоматически создаст пользователя и базу данных

---

## ❓ Если не знаете пароль postgres

Попробуйте следующие варианты:
- `postgres` (самый распространенный)
- Ваш пароль Windows
- Пустая строка (просто Enter)
- Пароль, который вы задавали при установке PostgreSQL

### Если ничего не помогает:

1. Откройте pgAdmin
2. Правой кнопкой на сервер PostgreSQL → Properties
3. Вкладка "Connection" - посмотрите сохраненный пароль
4. Или вкладка "General" → Change Password - измените пароль

---

## ✅ Проверка

После создания пользователя и БД, проверьте подключение:

```powershell
$env:PGPASSWORD = "qwerty"
psql -h localhost -p 5432 -U Gr1zBear -d CoPos -c "SELECT version();"
```

Если команда выполнилась без ошибок - все готово! Перезапустите Spring Boot приложение.

---

## 📝 Параметры подключения

После настройки используйте в `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/CoPos
spring.datasource.username=Gr1zBear
spring.datasource.password=qwerty
spring.datasource.driver-class-name=org.postgresql.Driver
```

---

## 🚀 После решения

Перезапустите Spring Boot приложение:
```bash
mvn spring-boot:run
```

Приложение должно запуститься без ошибок аутентификации!

