# Настройка системы сброса пароля

## Обзор

Система сброса пароля через email полностью интегрирована в backend и frontend приложения CoActivity.

## Backend компоненты

### 1. Модель данных
- `PasswordResetToken` - модель для хранения токенов сброса пароля
- Хранит токен, пользователя, дату истечения и статус использования

### 2. Сервисы
- `EmailService` - отправка email через SMTP
- `PasswordResetService` - логика сброса пароля (создание токена, валидация, обновление пароля)

### 3. API Endpoints
- `POST /password-reset/request` - запрос сброса пароля (принимает email)
- `POST /password-reset/confirm` - подтверждение сброса (принимает token и newPassword)
- `GET /password-reset/validate-token?token=...` - проверка валидности токена

## Настройка SMTP

### 1. Переменные окружения

Добавьте следующие переменные окружения или обновите `application.properties`:

```properties
# SMTP настройки
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@coactivity.com

# URL фронтенда для ссылок в письмах
app.password-reset.base-url=http://localhost:3000
```

### 2. Настройка Gmail (пример)

1. Включите двухфакторную аутентификацию в Google аккаунте
2. Создайте пароль приложения:
   - Перейдите в настройки аккаунта Google
   - Безопасность → Двухэтапная аутентификация → Пароли приложений
   - Создайте новый пароль для "Почта" и "Другое устройство"
   - Используйте этот пароль в `MAIL_PASSWORD`

3. Обновите `application.properties`:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${MAIL_PASSWORD:your-app-password}
```

### 3. Другие SMTP провайдеры

#### Mail.ru
```properties
spring.mail.host=smtp.mail.ru
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

#### Yandex
```properties
spring.mail.host=smtp.yandex.ru
spring.mail.port=465
spring.mail.properties.mail.smtp.ssl.enable=true
```

## Frontend компоненты

### 1. Страницы
- `ForgotPassword.jsx` - страница запроса сброса пароля
- `ResetPassword.jsx` - страница установки нового пароля

### 2. API интеграция
- `passwordResetAPI.requestReset(email)` - запрос сброса
- `passwordResetAPI.confirmReset(token, newPassword)` - подтверждение
- `passwordResetAPI.validateToken(token)` - валидация токена

### 3. Навигация
- Добавлены маршруты: `forgot-password` и `reset-password`
- Ссылка "Забыли пароль?" в форме входа

## Безопасность

1. **Токены**
   - Используются UUID для генерации токенов
   - Токены имеют срок действия (по умолчанию 24 часа)
   - Токены одноразовые (помечаются как использованные)

2. **Защита от перечисления email**
   - При запросе сброса всегда возвращается одинаковый успешный ответ
   - Не раскрывается, существует ли email в системе

3. **Валидация пароля**
   - Минимум 8 символов
   - Хеширование через BCrypt

## Тестирование

### 1. Локальное тестирование без SMTP

Для тестирования без реальной отправки email можно использовать Mock SMTP сервер:
- MailHog: https://github.com/mailhog/MailHog
- MailCatcher: https://mailcatcher.me/

### 2. Проверка работы

1. Запустите backend
2. Запустите frontend
3. Перейдите на страницу входа
4. Нажмите "Забыли пароль?"
5. Введите email зарегистрированного пользователя
6. Проверьте почту (или MailHog, если используете)
7. Перейдите по ссылке из письма
8. Установите новый пароль

## Устранение проблем

### Email не отправляется

1. Проверьте логи backend на наличие ошибок SMTP
2. Убедитесь, что переменные окружения установлены правильно
3. Для Gmail: используйте пароль приложения, не обычный пароль
4. Проверьте настройки firewall/антивируса
5. Убедитесь, что порт 587 не заблокирован

### Токен недействителен

1. Проверьте, что токен не истек (24 часа по умолчанию)
2. Убедитесь, что токен не был использован ранее
3. Проверьте правильность URL в письме

### Ошибки базы данных

1. Убедитесь, что таблица `password_reset_tokens` создана
2. Проверьте логи Hibernate/JPA на ошибки миграций

## Дополнительная настройка

### Изменение времени жизни токена

В `application.properties`:
```properties
app.password-reset.token-expiration-hours=48
```

### Изменение базового URL

В `application.properties`:
```properties
app.password-reset.base-url=https://yourdomain.com
```

## User Stories покрытие

- ✅ Пользователь может запросить сброс пароля через email
- ✅ Пользователь получает письмо с инструкциями
- ✅ Пользователь может установить новый пароль по ссылке из письма
- ✅ Система безопасно обрабатывает несуществующие email
- ✅ Токены имеют ограниченное время жизни
- ✅ Пароли хешируются при сохранении

