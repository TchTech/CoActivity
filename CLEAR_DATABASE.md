# Инструкция по очистке базы данных

## Способ 1: Через API (если приложение запущено)

После перезапуска Spring Boot приложения выполните:

```bash
# PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/admin/clear-database?confirm=yes" -Method POST

# или curl
curl -X POST "http://localhost:8080/admin/clear-database?confirm=yes"
```

## Способ 2: Через SQL скрипт

SQL скрипт расположен в: `src/main/resources/sql/clear_database.sql`

### Вариант A: Через Docker (если PostgreSQL в контейнере)

```bash
docker exec -i postgres psql -U Gr1zBear -d CoPos < src/main/resources/sql/clear_database.sql
```

### Вариант B: Через psql напрямую

```bash
psql -h localhost -p 5433 -U Gr1zBear -d CoPos -f src/main/resources/sql/clear_database.sql
```

Пароль: `qwerty`

### Вариант C: Через IDE (IntelliJ IDEA, DBeaver, pgAdmin)

1. Откройте файл `src/main/resources/sql/clear_database.sql`
2. Выполните скрипт в подключении к базе `CoPos`

## Способ 3: Через PowerShell скрипт

Запустите:
```bash
.\clear_db.ps1
```

## Способ 4: Через CommandLineRunner (при запуске приложения)

Запустите приложение с параметром:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.clear-database=true"
```

---

**ВНИМАНИЕ:** Все эти методы удалят ВСЕ данные из базы данных! Резервная копия не создается автоматически.

