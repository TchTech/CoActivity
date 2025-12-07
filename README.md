# CoActivity

Проект CoActivity - это многокомпонентное приложение для совместной деятельности, состоящее из:
- **Backend**: Spring Boot приложение на Java 21
- **Frontend**: Next.js приложение на React
- **ML сервис**: FastAPI сервис для рекомендаций комнат
- **База данных**: PostgreSQL

## 🚀 Быстрый старт (TL;DR)

```bash
# 1. Клонирование репозитория
git clone <repository-url>
cd CoActivity

# 2. Запуск PostgreSQL через Docker
docker run -d --name postgres -e POSTGRES_USER=Gr1zBear -e POSTGRES_PASSWORD=qwerty -e POSTGRES_DB=CoPos -p 5432:5432 postgres:17

# Или через Docker Compose (включает все сервисы)
docker-compose up -d

# 3. Запуск Backend (используйте Maven Wrapper - не требует установки Maven)
./mvnw spring-boot:run
# или на Windows:
.\mvnw.cmd spring-boot:run

# 4. Запуск Frontend (в новом терминале)
cd frontend
pnpm install  # или npm install
pnpm dev      # или npm run dev

# 5. Запуск ML сервиса (в новом терминале)
cd ml/fastapi
python -m venv venv

PowerShell: .\venv\Scripts\Activate.ps1
CMD: venv\Scripts\activate.bat
Git Bash: source venv/Scripts/activate
Linux: source venv/bin/activate

pip install -r requirements.txt
python main.py
```

**Проверка:**
- Backend: http://localhost:8080
- Frontend: http://localhost:3000
- ML API: http://localhost:8000/health

## 📋 Требования

**⚠️ Важно:** Проект использует фиксированные версии инструментов для обеспечения воспроизводимости. Используйте файлы `.java-version`, `.nvmrc`, `.tool-versions` для автоматической настройки версий через менеджеры версий.

### Обязательные компоненты

- **Java 21** (JDK) - см. `.java-version`
- **Maven 3.9+** (или используйте Maven Wrapper - `mvnw`/`mvnw.cmd`)
- **Node.js 20 LTS** - см. `.nvmrc`
- **pnpm** (рекомендуется) или npm
- **Python 3.11+** - см. `.tool-versions`
- **PostgreSQL 17** или **Docker**

### Опциональные компоненты

- **Docker Compose** - для запуска всех сервисов одновременно
- **asdf** / **nvm** / **sdkman** / **pyenv** - менеджеры версий (рекомендуется)

📖 **Детальная инструкция по настройке:** см. [README_SETUP.md](README_SETUP.md)

## Быстрый старт

### Шаг 1: Запуск базы данных PostgreSQL

**Вариант A: Использование Docker (рекомендуется)**

```bash
docker run -d --name psql-db \
  -e POSTGRES_USER=Gr1zBear \
  -e POSTGRES_PASSWORD=qwerty \
  -e POSTGRES_DB=CoPos \
  -p 5433:5432 \
  postgres:17
```

**Вариант B: Локальная установка PostgreSQL**

Если у вас установлен PostgreSQL локально и порт 5432 свободен:

1. Создайте базу данных и пользователя:
   ```sql
   CREATE DATABASE CoPos;
   CREATE USER Gr1zBear WITH PASSWORD 'qwerty';
   GRANT ALL PRIVILEGES ON DATABASE CoPos TO Gr1zBear;
   ```

2. Обновите `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/CoPos
   ```

**Параметры подключения:**
- **База данных:** `CoPos`
- **Пользователь:** `Gr1zBear`
- **Пароль:** `qwerty`
- **Порт:** `5433` (Docker) или `5432` (локально)
- **Хост:** `localhost`

### Шаг 2: Запуск Backend (Spring Boot)

```bash
# Используйте Maven Wrapper (рекомендуется - не требует установки Maven)
./mvnw clean package

# Запуск приложения
./mvnw spring-boot:run

# Или на Windows:
mvnw.cmd spring-boot:run

# Если Maven установлен глобально, можно использовать:
mvn clean package
mvn spring-boot:run
```

Backend будет доступен на `http://localhost:8080`

**Проверка работы:**
```bash
# Windows PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/users/register" -Method POST -Body @{username="test";email="test@test.com";password="test123"} -ContentType "application/x-www-form-urlencoded"

# Или используйте скрипт
.\quick-test.ps1
```

### Шаг 3: Запуск Frontend (Next.js)

```bash
cd frontend

# Установка зависимостей (используйте pnpm для воспроизводимости)
pnpm install
# или
npm install

# Создайте файл .env.local с настройками (опционально)
# NEXT_PUBLIC_API_BASE=http://localhost:8080
# NEXT_PUBLIC_BACKEND_URL=http://localhost:8080

# Запуск в режиме разработки
pnpm dev
# или
npm run dev
```

**Примечание:** Lock файлы (`pnpm-lock.yaml`, `package-lock.json`) зафиксированы в репозитории для обеспечения воспроизводимости установки зависимостей.

Frontend будет доступен на `http://localhost:3000`

### Шаг 4: Запуск ML сервиса (FastAPI)

```bash
cd ml/fastapi

# Создайте виртуальное окружение (рекомендуется)
python -m venv venv

# Активация виртуального окружения
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Установка зависимостей
pip install -r requirements.txt

# Запуск сервиса
python main.py
# или через uvicorn
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

**Примечание:** Версии зависимостей зафиксированы в `requirements.txt` для обеспечения воспроизводимости.

ML сервис будет доступен на `http://localhost:8000`

**Проверка работы:**
```bash
# Проверка здоровья
curl http://localhost:8000/health

# Или через PowerShell
Invoke-RestMethod -Uri "http://localhost:8000/health"

# Запуск тестов
python test.py
```

## Проверка работоспособности

После запуска всех компонентов проверьте:

### Backend API
- **URL:** `http://localhost:8080`
- **Тест регистрации:** `POST http://localhost:8080/users/register`
  ```bash
  # PowerShell
  Invoke-RestMethod -Uri "http://localhost:8080/users/register" -Method POST `
    -Body "username=testuser&email=test@example.com&password=test123" `
    -ContentType "application/x-www-form-urlencoded"
  ```

### Frontend
- **URL:** `http://localhost:3000`
- Откройте в браузере для визуальной проверки

### ML API
- **URL:** `http://localhost:8000`
- **Health Check:** `http://localhost:8000/health`
- **Документация:** `http://localhost:8000/docs` (Swagger UI)
- **Примеры комнат:** `http://localhost:8000/sample-rooms`

**Тестирование рекомендаций:**
```bash
# PowerShell
$body = @{interests = @("python", "программирование")} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8000/recommend-from-sample" `
  -Method POST -Body $body -ContentType "application/json"
```

## Структура проекта

```
CoActivity/
├── src/                    # Backend (Spring Boot)
│   └── main/
│       ├── java/          # Java исходники
│       │   ├── controller/  # REST контроллеры
│       │   ├── model/      # JPA модели
│       │   ├── repository/ # Репозитории
│       │   ├── service/    # Бизнес-логика
│       │   └── Security/   # Конфигурация безопасности
│       └── resources/     # Конфигурация и SQL скрипты
│           └── application.properties
├── frontend/              # Frontend (Next.js)
│   ├── app/              # Next.js App Router
│   ├── components/       # React компоненты
│   ├── hypertexts/       # Страницы приложения
│   └── package.json
├── ml/                    # ML сервис
│   └── fastapi/          # FastAPI приложение
│       ├── main.py       # Основной файл API
│       ├── test.py       # Тесты
│       ├── requirements.txt
│       └── russian.txt   # Стоп-слова для русского языка
├── compose.yaml          # Docker Compose конфигурация
├── Dockerfile            # Dockerfile для backend
├── pom.xml              # Maven конфигурация
└── README.md            # Этот файл
```

## API Эндпоинты

### Backend API (http://localhost:8080)

**Пользователи:**
- `POST /users/register` - Регистрация пользователя
  - Параметры: `username`, `email`, `password`
- `POST /users/subscribe` - Подписка на пользователя
  - Параметры: `userId`, `userToSubscribeId`

**Комнаты:**
- `POST /rooms/create` - Создание комнаты
  - Параметры: `userId`, `name`
- `POST /rooms/{roomId}/messages/add` - Добавление сообщения
  - Параметры: `creatorId`, `text`
- `POST /rooms/{roomId}/participants/add` - Добавление участника
  - Параметры: `userId`

**Посты:**
- `POST /posts/publish` - Публикация поста
- `POST /posts/{postId}/like` - Лайк поста
- `POST /posts/{postId}/dislike` - Дизлайк поста

**Комментарии:**
- `POST /comments/add` - Добавление комментария
- `GET /comments/{postId}` - Получение комментариев
- `DELETE /comments/delete` - Удаление комментария

### ML API (http://localhost:8000)

- `GET /` - Информация об API
- `GET /health` - Проверка здоровья сервиса
- `GET /sample-rooms` - Получение примеров комнат
- `POST /recommend-from-sample` - Рекомендации из примеров
  - Body: `{"interests": ["python", "программирование"]}`
- `POST /recommend` - Рекомендации с кастомными комнатами
  - Body: `{"user_interests": {...}, "rooms": [...]}`

## Полезные команды

### Backend
```bash
# Сборка без тестов
mvn clean package -DskipTests

# Запуск тестов
mvn test

# Очистка проекта
mvn clean

# Запуск с профилем
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend
```bash
# Сборка для продакшена
pnpm build

# Запуск продакшен версии
pnpm start

# Линтинг
pnpm lint

# Установка зависимостей заново
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

### ML сервис
```bash
# Запуск тестов
cd ml/fastapi
python test.py

# Запуск с uvicorn напрямую
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# Проверка зависимостей
pip list | grep -E "fastapi|uvicorn|scikit-learn|pandas|numpy"
```

### Docker
```bash
# Запуск PostgreSQL контейнера
docker run -d --name psql-db \
  -e POSTGRES_USER=Gr1zBear \
  -e POSTGRES_PASSWORD=qwerty \
  -e POSTGRES_DB=CoPos \
  -p 5433:5432 \
  postgres:17

# Остановка контейнера
docker stop psql-db

# Удаление контейнера
docker rm psql-db

# Просмотр логов
docker logs psql-db

# Проверка статуса
docker ps --filter "name=psql-db"
```

## Устранение проблем

### Docker

1. **Ошибка "unable to get image" или "cannot find the file specified":**
   - Убедитесь, что Docker Desktop запущен
   - Перезапустите Docker Desktop
   - Проверьте, что Docker Desktop полностью загрузился (иконка в трее зеленая)
   - Используйте ручной запуск PostgreSQL (см. выше)

2. **Ошибка "port is already allocated":**
   - Остановите контейнер: `docker stop psql-db`
   - Или используйте другой порт: `-p 5434:5432`

### База данных

1. **Ошибка подключения к БД:**
   - Убедитесь, что PostgreSQL запущен: `docker ps --filter "name=psql-db"`
   - Проверьте настройки в `src/main/resources/application.properties`
   - Убедитесь, что база данных `CoPos` создана
   - Проверьте порт (5433 для Docker, 5432 для локального)
   - Проверьте логин и пароль

2. **Порт 5432/5433 уже занят:**
   - Проверьте, что занимает порт: `netstat -ano | findstr :5433`
   - Остановите локальный PostgreSQL или измените порт Docker контейнера
   - Обновите `application.properties` с новым портом

3. **Ошибка аутентификации:**
   - Убедитесь, что контейнер создан с правильными переменными окружения
   - Пересоздайте контейнер с правильными параметрами

### Backend

1. **Порт 8080 уже занят:**
   - Измените `server.port` в `application.properties`
   - Или остановите процесс: `netstat -ano | findstr :8080`

2. **Ошибки компиляции:**
   - Убедитесь, что установлен Java 21: `java -version`
   - Выполните `mvn clean` и затем `mvn package`
   - Проверьте, что все зависимости загружены

3. **Ошибка "BCryptPasswordEncoder not found":**
   - Убедитесь, что зависимость `spring-security-crypto` добавлена в `pom.xml`
   - Выполните `mvn clean compile`

4. **Ошибки JPA/Hibernate:**
   - Проверьте, что все Entity классы имеют аннотацию `@Entity`
   - Убедитесь, что все связи правильно настроены
   - Проверьте наличие `@NoArgsConstructor` в Entity классах

### Frontend

1. **Ошибки зависимостей:**
   - Удалите `node_modules` и `pnpm-lock.yaml` (или `package-lock.json`)
   - Выполните `pnpm install` заново
   - Проверьте версию Node.js: `node -v` (должна быть 18+)

2. **Порт 3000 уже занят:**
   - Next.js автоматически предложит использовать другой порт
   - Или укажите порт явно: `pnpm dev -- -p 3001`

3. **Ошибки сборки:**
   - Проверьте версию Node.js и pnpm
   - Очистите кэш: `pnpm store prune`

### ML сервис

1. **ML сервис не запускается:**
   - Проверьте, что файл `russian.txt` существует в директории `ml/fastapi/`
   - Убедитесь, что все зависимости установлены: `pip install -r requirements.txt`
   - Проверьте версию Python: `python --version` (должна быть 3.8+)

2. **Ошибка импорта модулей:**
   - Убедитесь, что виртуальное окружение активировано (если используется)
   - Переустановите зависимости: `pip install -r requirements.txt --force-reinstall`
   - Проверьте, что вы находитесь в правильной директории

3. **Ошибка кодировки:**
   - Файл `russian.txt` должен быть в кодировке UTF-8
   - Код автоматически обрабатывает разные кодировки

4. **Порт 8000 уже занят:**
   - Остановите другой процесс на порту 8000
   - Или измените порт в `main.py`: `uvicorn.run(..., port=8001)`

5. **Предупреждения о стоп-словах:**
   - Это нормально, код автоматически фильтрует невалидные стоп-слова
   - Предупреждения не влияют на работу сервиса

## Тестирование

### Backend тесты
```bash
# Запуск всех тестов
mvn test

# Запуск конкретного теста
mvn test -Dtest=UserServiceTest
```

### ML сервис тесты
```bash
cd ml/fastapi
python test.py
```

### Ручное тестирование API

**Backend:**
```bash
# PowerShell
.\quick-test.ps1
# или
.\test-api.ps1
```

**ML сервис:**
```bash
# Проверка здоровья
curl http://localhost:8000/health

# Получение примеров комнат
curl http://localhost:8000/sample-rooms

# Тест рекомендаций
curl -X POST http://localhost:8000/recommend-from-sample \
  -H "Content-Type: application/json" \
  -d '{"interests": ["python", "программирование"]}'
```

## Порты по умолчанию

- **Backend:** `8080` (настраивается через `server.port` в `application.properties`)
- **Frontend:** `3000` (автоматически меняется при занятости порта)
- **ML API:** `8000` (настраивается в `main.py`)
- **PostgreSQL:** `5432` (Docker и локально)

## Версии инструментов

Проект использует фиксированные версии для обеспечения воспроизводимости:

- **Java:** 21 (см. `.java-version`)
- **Maven:** 3.9+ (используется Maven Wrapper)
- **Node.js:** 20 LTS (см. `.nvmrc`)
- **Python:** 3.11+ (см. `.tool-versions`)
- **PostgreSQL:** 17
- **Spring Boot:** 3.3.5 (см. `pom.xml`)
- **Next.js:** 16.0.0 (см. `frontend/package.json`)

Менеджеры версий автоматически используют эти файлы:
- **asdf** - читает `.tool-versions`
- **nvm** - читает `.nvmrc`
- **sdkman** - читает `.java-version`

## Разработка

### Добавление новых зависимостей

**Backend (Maven):**
```xml
<!-- Добавьте в pom.xml в секцию <dependencies> -->
<dependency>
    <groupId>...</groupId>
    <artifactId>...</artifactId>
    <version>...</version>
</dependency>
```

**Frontend (pnpm):**
```bash
pnpm add package-name
```

**ML сервис (pip):**
```bash
pip install package-name
# Добавьте в requirements.txt
pip freeze > requirements.txt
```

## Воспроизводимость

Проект настроен для обеспечения воспроизводимости на любой машине:

- ✅ Фиксированные версии всех инструментов (Java 21, Node.js 20, Python 3.11+)
- ✅ Lock файлы зависимостей зафиксированы в Git
- ✅ Maven Wrapper для воспроизводимой сборки
- ✅ Docker Compose для изолированной среды

📖 **Детальная информация:** см. [REPRODUCIBILITY.md](REPRODUCIBILITY.md)

## Документация

- [README_SETUP.md](README_SETUP.md) - Детальная инструкция по настройке
- [REPRODUCIBILITY.md](REPRODUCIBILITY.md) - Обеспечение воспроизводимости проекта
- [LIB_UTILS_SETUP.md](LIB_UTILS_SETUP.md) - Настройка lib и utils

## Лицензия

См. файл [LICENSE](LICENSE)

## Контакты и поддержка

При возникновении проблем:
1. Проверьте раздел "Устранение проблем"
2. Убедитесь, что все зависимости установлены
3. Проверьте версии инструментов (см. раздел "Версии инструментов")
4. Проверьте логи приложения
5. Убедитесь, что все сервисы запущены и доступны
6. См. [README_SETUP.md](README_SETUP.md) для детальной инструкции по настройке

## Воспроизводимость

Проект настроен для обеспечения воспроизводимости на любой машине:

- ✅ Фиксированные версии всех инструментов (Java 21, Node.js 20, Python 3.11+)
- ✅ Lock файлы зависимостей зафиксированы в Git
- ✅ Maven Wrapper для воспроизводимой сборки
- ✅ Docker Compose для изолированной среды

📖 **Детальная информация:** см. [REPRODUCIBILITY.md](REPRODUCIBILITY.md)

## Документация

- [README_SETUP.md](README_SETUP.md) - Детальная инструкция по настройке
- [REPRODUCIBILITY.md](REPRODUCIBILITY.md) - Обеспечение воспроизводимости проекта
6. См. [README_SETUP.md](README_SETUP.md) для детальной инструкции по настройке
