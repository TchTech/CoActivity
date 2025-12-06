# Быстрый старт: Frontend в продакшне

## Шаги для запуска:

```bash
cd frontend

# 1. Установка зависимостей
pnpm install

# 2. Создайте файл .env.production (если нужно изменить URL бэкенда)
# BACKEND_URL=http://your-backend:8080

# 3. Сборка
pnpm build

# 4. Запуск
pnpm start
```

Сервер будет доступен на `http://localhost:3000`

**Примечание:** По умолчанию бэкенд: `http://localhost:8080`. Чтобы изменить, установите переменную окружения `BACKEND_URL`.

## Docker:

```bash
# Сборка
docker build -t coactivity-frontend ./frontend

# Запуск
docker run -d --name coactivity-frontend -p 3000:3000 \
  -e BACKEND_URL=http://your-backend:8080 \
  coactivity-frontend
```

Подробная документация: [PRODUCTION.md](./PRODUCTION.md)

