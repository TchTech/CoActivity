# PowerShell скрипт для очистки базы данных CoPos
# Требуется PostgreSQL клиент (psql) или Docker

Write-Host "========================================"
Write-Host "Очистка базы данных CoPos"
Write-Host "========================================"
Write-Host ""
Write-Host "ВНИМАНИЕ: Этот скрипт удалит ВСЕ данные из базы данных!"
Write-Host ""

$confirm = Read-Host "Вы уверены? (yes/no)"

if ($confirm -ne "yes") {
    Write-Host "Операция отменена."
    exit
}

Write-Host ""
Write-Host "Подключение к базе данных..."

# Проверяем наличие Docker
$dockerExists = Get-Command docker -ErrorAction SilentlyContinue
$psqlExists = Get-Command psql -ErrorAction SilentlyContinue

$sqlScript = Get-Content "src\main\resources\sql\clear_database.sql" -Raw

if ($dockerExists) {
    # Пробуем через Docker
    Write-Host "Использование Docker для подключения к PostgreSQL..."
    try {
        $result = $sqlScript | docker exec -i postgres psql -U Gr1zBear -d CoPos 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "========================================"
            Write-Host "База данных успешно очищена!"
            Write-Host "========================================"
        } else {
            Write-Host "Ошибка при выполнении через Docker:"
            Write-Host $result
        }
    } catch {
        Write-Host "Не удалось выполнить через Docker. Проверьте, что контейнер postgres запущен."
        Write-Host "Ошибка: $_"
    }
} elseif ($psqlExists) {
    # Пробуем напрямую через psql
    Write-Host "Использование psql для подключения к PostgreSQL..."
    $env:PGPASSWORD = "qwerty"
    try {
        $result = $sqlScript | psql -h localhost -p 5433 -U Gr1zBear -d CoPos 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "========================================"
            Write-Host "База данных успешно очищена!"
            Write-Host "========================================"
        } else {
            Write-Host "Ошибка при выполнении:"
            Write-Host $result
        }
    } catch {
        Write-Host "Не удалось выполнить через psql."
        Write-Host "Ошибка: $_"
    }
} else {
    Write-Host "ОШИБКА: Не найден ни Docker, ни psql."
    Write-Host ""
    Write-Host "Установите PostgreSQL или Docker, или выполните SQL скрипт вручную:"
    Write-Host "  Файл: src\main\resources\sql\clear_database.sql"
    Write-Host ""
    Write-Host "Или используйте API эндпоинт (если приложение запущено):"
    Write-Host "  POST http://localhost:8080/admin/clear-database?confirm=yes"
}

