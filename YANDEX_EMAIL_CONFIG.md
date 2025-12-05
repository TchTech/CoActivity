# Настройка Yandex почты - выполнено ✅

## Конфигурация

Почта Yandex успешно встроена в проект CoActivity:

- **Email отправителя**: coactivity@yandex.com
- **SMTP сервер**: smtp.yandex.ru
- **Порт**: 587 (STARTTLS)
- **Аутентификация**: Включена

## Текущие настройки

Все настройки находятся в `src/main/resources/application.properties`:

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

## Как проверить работу

1. **Запустите backend**:
   ```bash
   mvn spring-boot:run
   ```

2. **Создайте тестового пользователя** (если еще нет):
   ```bash
   POST http://localhost:8080/users/register
   Body: username=testuser&email=test@example.com&password=test123456
   ```

3. **Запросите сброс пароля**:
   ```bash
   POST http://localhost:8080/password-reset/request
   Body: {"email": "test@example.com"}
   ```

4. **Проверьте почтовый ящик** `coactivity@yandex.com` или почту получателя для письма со ссылкой на сброс пароля.

## Важно для Yandex

Если при отправке возникают ошибки:

1. **Проверьте пароль**: Для Yandex может потребоваться пароль приложения, если включена двухфакторная аутентификация
   - Создайте пароль приложения: https://id.yandex.ru/security → Пароли приложений
   - Замените пароль в `application.properties`

2. **Альтернативный порт**: Если порт 587 не работает, используйте 465:
   ```properties
   spring.mail.port=465
   spring.mail.properties.mail.smtp.ssl.enable=true
   spring.mail.properties.mail.smtp.starttls.enable=false
   ```

## Логи

Успешная отправка будет видна в логах:
```
INFO ... EmailService - Password reset email sent to: test@example.com
```

Ошибки также будут видны:
```
ERROR ... EmailService - Failed to send password reset email to: ...
```

## Готово к использованию! 🚀

Система сброса пароля через Yandex почту полностью настроена и готова к работе.

