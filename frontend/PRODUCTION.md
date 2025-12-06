# Инструкция по запуску Frontend в продакшне

## Предварительные требования

- Node.js 18+ установлен
- pnpm установлен глобально: `npm install -g pnpm`

## Шаг 1: Установка зависимостей

```bash
cd frontend

# Установка зависимостей в продакшн-режиме (без devDependencies)
pnpm install --prod=false

# Или с флагом для продакшн-окружения
pnpm install --prod
```

## Шаг 2: Настройка переменных окружения

Создайте файл `.env.production` в папке `frontend/`:

```env
# URL бэкенд-сервера
BACKEND_URL=http://your-backend-server:8080
NEXT_PUBLIC_BACKEND_URL=http://your-backend-server:8080

# Порт для запуска Next.js (опционально)
PORT=3000
```

**Важно:** В продакшне обязательно укажите правильный URL вашего бэкенд-сервера.

## Шаг 3: Сборка проекта

```bash
# Сборка оптимизированной версии для продакшна
pnpm build
```

Эта команда:
- Оптимизирует код и бандлы
- Создаёт статические страницы где возможно
- Генерирует production-версию в папке `.next/`

## Шаг 4: Запуск production-сервера

```bash
# Запуск production-сервера Next.js
pnpm start
```

Сервер будет доступен на `http://localhost:3000` (или на порту, указанном в переменной окружения `PORT`).

## Альтернативные варианты запуска

### Вариант 1: Запуск на определённом порту

```bash
PORT=8080 pnpm start
```

### Вариант 2: Запуск в фоновом режиме (Windows PowerShell)

```powershell
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; pnpm start"
```

### Вариант 3: Использование PM2 для управления процессом

```bash
# Установка PM2
npm install -g pm2

# Запуск с PM2
cd frontend
pm2 start npm --name "coactivity-frontend" -- start

# Просмотр статуса
pm2 status

# Просмотр логов
pm2 logs coactivity-frontend

# Перезапуск
pm2 restart coactivity-frontend

# Остановка
pm2 stop coactivity-frontend
```

## Docker-развёртывание

Создайте `Dockerfile` в папке `frontend/`:

```dockerfile
# Этап сборки
FROM node:18-alpine AS builder

WORKDIR /app

# Копируем файлы зависимостей
COPY package.json pnpm-lock.yaml* ./

# Устанавливаем pnpm
RUN npm install -g pnpm

# Устанавливаем зависимости
RUN pnpm install --frozen-lockfile

# Копируем исходный код
COPY . .

# Собираем приложение
RUN pnpm build

# Продакшн-этап
FROM node:18-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production

# Устанавливаем pnpm
RUN npm install -g pnpm

# Копируем файлы из builder
COPY --from=builder /app/package.json ./
COPY --from=builder /app/pnpm-lock.yaml* ./
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/next.config.mjs ./
COPY --from=builder /app/node_modules ./node_modules

EXPOSE 3000

ENV PORT=3000

CMD ["pnpm", "start"]
```

Затем соберите и запустите контейнер:

```bash
# Сборка образа
docker build -t coactivity-frontend ./frontend

# Запуск контейнера
docker run -d \
  --name coactivity-frontend \
  -p 3000:3000 \
  -e BACKEND_URL=http://your-backend:8080 \
  coactivity-frontend
```

## Проверка работы

1. Откройте браузер и перейдите на `http://localhost:3000`
2. Проверьте консоль браузера на наличие ошибок
3. Проверьте логи сервера на наличие ошибок подключения к бэкенду

## Оптимизация для продакшна

### Очистка кеша

```bash
# Удаление кеша сборки
rm -rf .next
pnpm build
```

### Анализ размера бандла

```bash
# Установка анализатора
pnpm add -D @next/bundle-analyzer

# Добавьте в next.config.mjs:
# const withBundleAnalyzer = require('@next/bundle-analyzer')({
#   enabled: process.env.ANALYZE === 'true',
# })

# Запуск анализа
ANALYZE=true pnpm build
```

## Troubleshooting

### Проблема: Ошибки подключения к бэкенду

**Решение:** Проверьте переменную окружения `BACKEND_URL` и убедитесь, что бэкенд доступен.

### Проблема: Порт уже занят

**Решение:** Измените порт через переменную окружения `PORT` или остановите процесс, использующий порт 3000.

### Проблема: Ошибки сборки

**Решение:** 
```bash
# Очистка и переустановка
rm -rf .next node_modules pnpm-lock.yaml
pnpm install
pnpm build
```

## Мониторинг и логирование

В продакшне рекомендуется настроить:
- Логирование ошибок (Sentry, LogRocket и т.д.)
- Мониторинг производительности (Vercel Analytics уже подключен)
- Мониторинг доступности сервера

## Рекомендации по безопасности

1. Не храните секреты в `.env.production` в репозитории
2. Используйте переменные окружения на сервере
3. Настройте HTTPS в продакшне (через reverse proxy: nginx, Caddy и т.д.)
4. Настройте CORS на бэкенде для разрешённых доменов

