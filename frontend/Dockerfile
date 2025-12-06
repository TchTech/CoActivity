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

