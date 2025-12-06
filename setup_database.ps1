# PowerShell скрипт для настройки PostgreSQL базы данных
# Требуется: PostgreSQL должен быть установлен и доступен

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Настройка PostgreSQL для CoActivity" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Проверка наличия psql
$psqlPath = Get-Command psql -ErrorAction SilentlyContinue
if (-not $psqlPath) {
    Write-Host "ОШИБКА: psql не найден в PATH" -ForegroundColor Red
    Write-Host "Пожалуйста, добавьте PostgreSQL bin директорию в PATH" -ForegroundColor Yellow
    Write-Host "Или укажите полный путь к psql.exe" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Пример:" -ForegroundColor Yellow
    Write-Host '  $env:Path += ";C:\Program Files\PostgreSQL\17\bin"' -ForegroundColor Gray
    exit 1
}

Write-Host "Найден psql: $($psqlPath.Source)" -ForegroundColor Green
Write-Host ""

# Попытка подключения к PostgreSQL
Write-Host "Попытка подключения к PostgreSQL..." -ForegroundColor Yellow

# Пробуем подключиться как postgres (суперпользователь)
$env:PGPASSWORD = Read-Host -Prompt "Введите пароль пользователя postgres (или нажмите Enter для стандартного)"

if ([string]::IsNullOrWhiteSpace($env:PGPASSWORD)) {
    Write-Host "Попытка подключения без пароля..." -ForegroundColor Yellow
}

$connectionTest = & psql -h localhost -p 5432 -U postgres -d postgres -c "SELECT version();" 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "ОШИБКА: Не удалось подключиться к PostgreSQL" -ForegroundColor Red
    Write-Host "Проверьте:" -ForegroundColor Yellow
    Write-Host "  1. PostgreSQL запущен" -ForegroundColor Yellow
    Write-Host "  2. Пароль пользователя postgres правильный" -ForegroundColor Yellow
    Write-Host "  3. PostgreSQL слушает на порту 5432" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Попробуйте запустить скрипт снова с правильным паролем:" -ForegroundColor Yellow
    Write-Host '  $env:PGPASSWORD="your_password"; .\setup_database.ps1' -ForegroundColor Gray
    exit 1
}

Write-Host "Подключение успешно!" -ForegroundColor Green
Write-Host ""

# Создание пользователя
Write-Host "Создание пользователя Gr1zBear..." -ForegroundColor Yellow
$createUser = @"
DO `$`$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'Gr1zBear') THEN
        CREATE USER "Gr1zBear" WITH PASSWORD 'qwerty';
        RAISE NOTICE 'User Gr1zBear created';
    ELSE
        RAISE NOTICE 'User Gr1zBear already exists';
        ALTER USER "Gr1zBear" WITH PASSWORD 'qwerty';
        RAISE NOTICE 'Password updated for Gr1zBear';
    END IF;
END
`$`$;
"@

$createUserResult = $createUser | & psql -h localhost -p 5432 -U postgres -d postgres 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Пользователь создан/обновлен успешно!" -ForegroundColor Green
} else {
    Write-Host "Предупреждение при создании пользователя:" -ForegroundColor Yellow
    Write-Host $createUserResult
}

Write-Host ""

# Создание базы данных
Write-Host "Создание базы данных CoPos..." -ForegroundColor Yellow
$checkDb = & psql -h localhost -p 5432 -U postgres -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='CoPos'" 2>&1

if ([string]::IsNullOrWhiteSpace($checkDb)) {
    $createDb = & psql -h localhost -p 5432 -U postgres -d postgres -c 'CREATE DATABASE "CoPos" OWNER "Gr1zBear";' 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "База данных CoPos создана успешно!" -ForegroundColor Green
    } else {
        Write-Host "ОШИБКА при создании базы данных:" -ForegroundColor Red
        Write-Host $createDb
        exit 1
    }
} else {
    Write-Host "База данных CoPos уже существует" -ForegroundColor Yellow
}

Write-Host ""

# Выдача привилегий
Write-Host "Настройка привилегий..." -ForegroundColor Yellow
$grantPrivileges = @"
GRANT ALL PRIVILEGES ON DATABASE "CoPos" TO "Gr1zBear";
\c "CoPos"
GRANT ALL ON SCHEMA public TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO "Gr1zBear";
"@

$grantResult = $grantPrivileges | & psql -h localhost -p 5432 -U postgres -d postgres 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Привилегии выданы успешно!" -ForegroundColor Green
} else {
    Write-Host "Предупреждение при выдаче привилегий:" -ForegroundColor Yellow
    Write-Host $grantResult
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Настройка завершена!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Параметры подключения:" -ForegroundColor Cyan
Write-Host "  База данных: CoPos" -ForegroundColor White
Write-Host "  Пользователь: Gr1zBear" -ForegroundColor White
Write-Host "  Пароль: qwerty" -ForegroundColor White
Write-Host "  Порт: 5432" -ForegroundColor White
Write-Host ""
Write-Host "Теперь можно запускать Spring Boot приложение!" -ForegroundColor Green

