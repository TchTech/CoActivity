# Настройка Yandex почты для CoActivity

## Текущая конфигурация

Почта Yandex настроена и встроена в проект:

- **Email**: coactivity@yandex.com
- **SMTP сервер**: smtp.yandex.ru
- **Порт**: 587 (STARTTLS)
- **Протокол**: SMTP с аутентификацией

## Важные замечания для Yandex

### 1. Пароль приложения

Если используется обычный пароль аккаунта, для безопасности рекомендуется создать пароль приложения:

1. Перейдите на https://id.yandex.ru/security
2. Включите двухфакторную аутентификацию (если еще не включена)
3. Создайте пароль приложения:
   - Нажмите "Пароли приложений"
   - Выберите "Почта" и "Другое устройство"
   - Скопируйте сгенерированный пароль
   - Замените `iyprxalrsumoklof` на новый пароль приложения в `application.properties`

### 2. Настройки безопасности Yandex

Если возникают проблемы с отправкой:

1. Проверьте, что в настройках безопасности Yandex включен доступ для приложений
2. Убедитесь, что используется правильный пароль (пароль приложения или основной пароль)
3. Проверьте, не заблокирован ли доступ к почте со стороны Yandex

## Проверка работы

### Тест через API

1. Запустите backend:
   ```bash
   mvn spring-boot:run
   ```

2. Создайте тестового пользователя или используйте существующего

3. Отправьте запрос на сброс пароля:
   ```bash
   curl -X POST http://localhost:8080/password-reset/request \
     -H "Content-Type: application/json" \
     -d '{"email":"test@example.com"}'
   ```

4. Проверьте почтовый ящик coactivity@yandex.com для получения писем

### Логи

При отправке email проверяйте логи Spring Boot приложения:
```
INFO  ... EmailService - Password reset email sent to: test@example.com
```

Если есть ошибки, они будут отображены в логах:
```
ERROR ... EmailService - Failed to send password reset email to: ...
```

## Альтернативная конфигурация (порт 465)

Если порт 587 не работает, можно использовать порт 465 с SSL:

```properties
spring.mail.port=465
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.starttls.enable=false
```

## Устранение проблем

### Ошибка: "535 5.7.8 Error: authentication failed"

- Проверьте правильность логина и пароля
- Убедитесь, что используется пароль приложения (если включена 2FA)
- Проверьте, что в настройках безопасности Yandex разрешен доступ для приложений

### Ошибка: "Connection timeout"

- Проверьте, не блокирует ли firewall порт 587
- Убедитесь, что интернет-соединение стабильно
- Попробуйте использовать порт 465 с SSL

### Письма не доставляются

- Проверьте папку "Спам" в почтовом ящике получателя
- Убедитесь, что адрес отправителя (coactivity@yandex.com) корректный
- Проверьте логи на наличие ошибок отправки

## Безопасность

⚠️ **Внимание**: В текущей конфигурации пароль хранится в открытом виде в `application.properties`. 

Для продакшена рекомендуется:

1. Использовать переменные окружения:
   ```properties
   spring.mail.username=${MAIL_USERNAME}
   spring.mail.password=${MAIL_PASSWORD}
   ```

2. Или использовать Spring Cloud Config Server для централизованного хранения секретов

3. Никогда не коммитьте файлы с паролями в Git

## Текущие настройки в application.properties

```properties
spring.mail.host=smtp.yandex.ru
spring.mail.port=587
spring.mail.username=coactivity@yandex.com
spring.mail.password=iyprxalrsumoklof
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
app.mail.from=coactivity@yandex.com
```

Все настройки готовы к использованию! 🚀

