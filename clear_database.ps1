# Скрипт для очистки базы данных CoPos
param(
    [switch]$Help
)

if ($Help) {
    Write-Host "Использование:" -ForegroundColor Cyan
    Write-Host "  .\clear_database.ps1              - Очистка через Spring Boot" -ForegroundColor White
    Write-Host "  .\clear_database.ps1 -Direct      - Прямое выполнение SQL (требует psql)" -ForegroundColor White
    Write-Host ""
    exit 0
}

Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Очистка базы данных CoPos" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""

$SQL_SCRIPT = "src\main\resources\sql\clear_database.sql"

if (-not (Test-Path $SQL_SCRIPT)) {
    Write-Host "ОШИБКА: SQL скрипт не найден: $SQL_SCRIPT" -ForegroundColor Red
    exit 1
}

Write-Host "SQL скрипт найден: $SQL_SCRIPT" -ForegroundColor Green
Write-Host ""
Write-Host "Способ 1: Через Spring Boot приложение" -ForegroundColor Cyan
Write-Host "----------------------------------------" -ForegroundColor Gray
Write-Host "Выполните следующую команду:" -ForegroundColor White
Write-Host ""
Write-Host "  mvn spring-boot:run -Dspring-boot.run.arguments=`"--app.clear-database=true`"" -ForegroundColor Yellow
Write-Host ""
Write-Host "Приложение очистит базу данных и автоматически остановится." -ForegroundColor Gray
Write-Host ""
Write-Host "Способ 2: Прямое выполнение SQL (требует psql)" -ForegroundColor Cyan
Write-Host "----------------------------------------" -ForegroundColor Gray
Write-Host "Выполните:" -ForegroundColor White
Write-Host ""
Write-Host "  `$env:PGPASSWORD='qwerty'" -ForegroundColor Yellow
Write-Host "  psql -h localhost -U Gr1zBear -d CoPos -f `"$SQL_SCRIPT`"" -ForegroundColor Yellow
Write-Host ""
Write-Host "Способ 3: Через Docker (если база данных в контейнере)" -ForegroundColor Cyan
Write-Host "----------------------------------------" -ForegroundColor Gray
Write-Host "Выполните:" -ForegroundColor White
Write-Host ""
Write-Host "  Get-Content `"$SQL_SCRIPT`" -Raw | docker exec -i postgres psql -U Gr1zBear -d CoPos" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
