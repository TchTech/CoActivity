# Инструкция по настройке проекта CoActivity

Этот документ содержит детальные инструкции для настройки проекта на любой машине с нуля.

## Системные требования

### Обязательные компоненты

1. **Java Development Kit (JDK) 21**
   - Скачать: https://www.oracle.com/java/technologies/downloads/#java21
   - Или через SDKMAN: `sdk install java 21.0.1-tem`
   - Проверка: `java -version` (должна быть версия 21.x.x)

2. **Apache Maven 3.9+**
   - Скачать: https://maven.apache.org/download.cgi
   - Или через SDKMAN: `sdk install maven`
   - Проверка: `mvn -version` (должна быть версия 3.9+)
   - **Примечание:** Проект включает Maven Wrapper (`mvnw`), можно использовать его без установки Maven

3. **Node.js 20 LTS**
   - Скачать: https://nodejs.org/
   - Или через nvm: `nvm install 20` (используйте `.nvmrc` файл в проекте)
   - Проверка: `node -v` (должна быть версия 20.x.x)

4. **pnpm** (рекомендуется) или npm
   - Установка pnpm: `npm install -g pnpm`
   - Проверка: `pnpm -v`

5. **Python 3.11+**
   - Скачать: https://www.python.org/downloads/
   - Или через pyenv: `pyenv install 3.11.0`
   - Проверка: `python --version` (должна быть версия 3.11+)

6. **PostgreSQL 17** (или Docker)
   - PostgreSQL: https://www.postgresql.org/download/
   - Docker: https://www.docker.com/get-started
   - Проверка: `psql --version` или `docker --version`

### Опциональные компоненты (для удобства)

- **asdf** - менеджер версий (автоматически использует `.tool-versions`)
- **nvm** - менеджер версий Node.js (автоматически использует `.nvmrc`)
- **sdkman** - менеджер версий Java/Maven
- **pyenv** - менеджер версий Python

## Быстрая настройка через менеджеры версий

### Использование asdf (рекомендуется)

```bash
# Установите asdf: https://asdf-vm.com/guide/getting-started.html

# Установите плагины
asdf plugin add java
asdf plugin add nodejs
asdf plugin add python

# Установите версии (автоматически из .tool-versions)
asdf install

# Активируйте версии
asdf reshim
```

### Использование nvm для Node.js

```bash
# Установите nvm: https://github.com/nvm-sh/nvm

# Установите версию из .nvmrc
nvm install
nvm use
```

## Пошаговая настройка

### Шаг 1: Клонирование репозитория

```bash
git clone <repository-url>
cd CoActivity
```

### Шаг 2: Настройка базы данных PostgreSQL

#### Вариант A: Через Docker (рекомендуется)

```bash
# Запустите PostgreSQL контейнер
docker run -d \
  --name postgres \
  -e POSTGRES_USER=Gr1zBear \
  -e POSTGRES_PASSWORD=qwerty \
  -e POSTGRES_DB=CoPos \
  -p 5432:5432 \
  postgres:17

# Проверка
docker ps --filter "name=postgres"
```

#### Вариант B: Локальная установка PostgreSQL

```bash
# Создайте базу данных и пользователя
psql -U postgres

CREATE DATABASE CoPos;
CREATE USER Gr1zBear WITH PASSWORD 'qwerty';
GRANT ALL PRIVILEGES ON DATABASE CoPos TO Gr1zBear;
\q
```

#### Вариант C: Через Docker Compose

```bash
docker-compose up -d postgres
```

### Шаг 3: Настройка Backend (Spring Boot)

```bash
# Используйте Maven Wrapper (не требует установки Maven)
./mvnw clean install

# Или если Maven установлен глобально
mvn clean install

# Запуск приложения
./mvnw spring-boot:run
# или
mvn spring-boot:run
```

**Проверка:** Откройте http://localhost:8080

**Настройка через переменные окружения (опционально):**

Создайте файл `application-local.properties` в `src/main/resources/` или используйте переменные окружения:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/CoPos
export SPRING_DATASOURCE_USERNAME=Gr1zBear
export SPRING_DATASOURCE_PASSWORD=qwerty
export SERVER_PORT=8080
```

### Шаг 4: Настройка Frontend (Next.js)

```bash
cd frontend

# Установка зависимостей
pnpm install
# или
npm install

# Создайте .env.local файл (скопируйте из .env.example если есть)
# NEXT_PUBLIC_API_BASE=http://localhost:8080
# NEXT_PUBLIC_BACKEND_URL=http://localhost:8080

# Запуск в режиме разработки
pnpm dev
# или
npm run dev
```

**Проверка:** Откройте http://localhost:3000

### Шаг 5: Настройка ML сервиса (FastAPI)

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

**Проверка:** Откройте http://localhost:8000/health

## Проверка работоспособности

После запуска всех компонентов выполните проверки:

### 1. Backend API

```bash
# Проверка здоровья
curl http://localhost:8080

# Тест регистрации
curl -X POST http://localhost:8080/users/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser&email=test@example.com&password=test123"
```

### 2. Frontend

Откройте браузер: http://localhost:3000

### 3. ML API

```bash
# Проверка здоровья
curl http://localhost:8000/health

# Тест рекомендаций
curl -X POST http://localhost:8000/recommend-from-sample \
  -H "Content-Type: application/json" \
  -d '{"interests": ["python", "программирование"]}'
```

## Порты по умолчанию

- **Backend:** 8080
- **Frontend:** 3000
- **ML API:** 8000
- **PostgreSQL:** 5432

Если порты заняты, измените их в соответствующих конфигурационных файлах.

## Переменные окружения

### Backend

См. `src/main/resources/application.properties` или используйте переменные окружения:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `APP_RECOMMENDATION_API_URL`

### Frontend

Создайте файл `frontend/.env.local`:
- `NEXT_PUBLIC_API_BASE` - URL backend API
- `NEXT_PUBLIC_BACKEND_URL` - URL backend API
- `BACKEND_URL` - URL backend API

### ML Service

Создайте файл `ml/fastapi/.env`:
- `API_HOST` - хост для FastAPI (по умолчанию: 0.0.0.0)
- `API_PORT` - порт для FastAPI (по умолчанию: 8000)

## Структура проекта

```
CoActivity/
├── .java-version          # Версия Java (для sdkman/jenv)
├── .nvmrc                 # Версия Node.js (для nvm)
├── .tool-versions         # Версии инструментов (для asdf)
├── .gitignore             # Игнорируемые файлы Git
├── pom.xml                # Maven конфигурация
├── mvnw, mvnw.cmd         # Maven Wrapper
├── compose.yaml           # Docker Compose конфигурация
├── src/                   # Backend (Spring Boot)
│   └── main/
│       ├── java/          # Java исходники
│       └── resources/     # Конфигурация и ресурсы
│           └── application.properties
├── frontend/              # Frontend (Next.js)
│   ├── package.json
│   ├── pnpm-lock.yaml     # Lock файл зависимостей
│   └── .env.example       # Пример конфигурации
└── ml/                    # ML сервис
    └── fastapi/
        ├── requirements.txt
        └── .env.example   # Пример конфигурации
```

## Устранение проблем

### Проблема: Неправильная версия Java

```bash
# Проверьте версию
java -version

# Если не Java 21, используйте менеджер версий:
# asdf
asdf install java 21.0.1-tem
asdf global java 21.0.1-tem

# или sdkman
sdk install java 21.0.1-tem
sdk use java 21.0.1-tem
```

### Проблема: Неправильная версия Node.js

```bash
# Проверьте версию
node -v

# Если не Node.js 20, используйте nvm:
nvm install 20
nvm use 20

# или asdf
asdf install nodejs 20
asdf global nodejs 20
```

### Проблема: Maven не найден

Используйте Maven Wrapper:
```bash
./mvnw clean install
# Windows:
mvnw.cmd clean install
```

### Проблема: Зависимости не устанавливаются

**Backend:**
```bash
./mvnw clean install -U
```

**Frontend:**
```bash
cd frontend
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

**ML Service:**
```bash
cd ml/fastapi
pip install -r requirements.txt --force-reinstall
```

### Проблема: База данных не подключается

1. Проверьте, что PostgreSQL запущен:
   ```bash
   docker ps --filter "name=postgres"
   # или
   psql -U postgres -c "SELECT 1;"
   ```

2. Проверьте параметры подключения в `application.properties`

3. Проверьте, что база данных создана:
   ```sql
   psql -U postgres -l | grep CoPos
   ```

### Проблема: Порт занят

Найдите процесс, использующий порт:
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8080
kill -9 <PID>
```

Или измените порт в конфигурации.

## Дополнительные ресурсы

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Next.js Documentation](https://nextjs.org/docs)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Поддержка

При возникновении проблем:
1. Проверьте этот документ
2. Проверьте логи приложения
3. Убедитесь, что все версии инструментов соответствуют требованиям
4. Проверьте, что все сервисы запущены

