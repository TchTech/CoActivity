# Быстрая настройка PostgreSQL для CoActivity
# Этот скрипт создает пользователя и базу данных

$env:Path += ";C:\Program Files\PostgreSQL\18\bin"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Быстрая настройка PostgreSQL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Запрашиваем пароль postgres
Write-Host "Введите пароль пользователя postgres:" -ForegroundColor Yellow
Write-Host "(Обычно это: postgres, ваш_пароль_windows, или просто Enter для пустого)" -ForegroundColor Gray
$postgresPassword = Read-Host -AsSecureString
$postgresPasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($postgresPassword)
)

if ([string]::IsNullOrWhiteSpace($postgresPasswordPlain)) {
    Write-Host "Попытка подключения без пароля..." -ForegroundColor Yellow
    $env:PGPASSWORD = ""
} else {
    $env:PGPASSWORD = $postgresPasswordPlain
}

Write-Host ""

# Проверка подключения
Write-Host "Проверка подключения к PostgreSQL..." -ForegroundColor Yellow
$testConnection = & psql -h localhost -p 5432 -U postgres -d postgres -c "SELECT version();" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ОШИБКА: Не удалось подключиться к PostgreSQL" -ForegroundColor Red
    Write-Host $testConnection -ForegroundColor Red
    Write-Host ""
    Write-Host "Попробуйте:" -ForegroundColor Yellow
    Write-Host "1. Проверить, что PostgreSQL запущен" -ForegroundColor White
    Write-Host "2. Запустить скрипт снова с правильным паролем" -ForegroundColor White
    Write-Host "3. Или использовать pgAdmin для создания пользователя вручную" -ForegroundColor White
    exit 1
}

Write-Host "Подключение успешно!" -ForegroundColor Green
Write-Host ""

# Создание пользователя
Write-Host "Создание пользователя Gr1zBear..." -ForegroundColor Yellow
$createUserSQL = @"
DO `$`$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'Gr1zBear') THEN
        CREATE USER "Gr1zBear" WITH PASSWORD 'qwerty';
        RAISE NOTICE 'User Gr1zBear created successfully';
    ELSE
        ALTER USER "Gr1zBear" WITH PASSWORD 'qwerty';
        RAISE NOTICE 'User Gr1zBear already exists, password updated';
    END IF;
END
`$`$;
"@

$createUserResult = $createUserSQL | & psql -h localhost -p 5432 -U postgres -d postgres 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Пользователь Gr1zBear создан/обновлен!" -ForegroundColor Green
} else {
    Write-Host "Предупреждение при создании пользователя:" -ForegroundColor Yellow
    Write-Host $createUserResult
}

Write-Host ""

# Создание базы данных
Write-Host "Создание базы данных CoPos..." -ForegroundColor Yellow
$checkDb = & psql -h localhost -p 5432 -U postgres -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='CoPos'" 2>&1 | Out-String
$checkDb = $checkDb.Trim()

if ([string]::IsNullOrWhiteSpace($checkDb)) {
    $createDb = & psql -h localhost -p 5432 -U postgres -d postgres -c 'CREATE DATABASE "CoPos" OWNER "Gr1zBear";' 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "База данных CoPos создана!" -ForegroundColor Green
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
$grantSQL = @"
GRANT ALL PRIVILEGES ON DATABASE "CoPos" TO "Gr1zBear";
\c "CoPos"
GRANT ALL ON SCHEMA public TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO "Gr1zBear";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO "Gr1zBear";
"@

$grantResult = $grantSQL | & psql -h localhost -p 5432 -U postgres -d postgres 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Привилегии выданы!" -ForegroundColor Green
} else {
    Write-Host "Предупреждение при выдаче привилегий:" -ForegroundColor Yellow
    Write-Host $grantResult
}

Write-Host ""

# Проверка подключения с новым пользователем
Write-Host "Проверка подключения с пользователем Gr1zBear..." -ForegroundColor Yellow
$env:PGPASSWORD = "qwerty"
$testUserConnection = & psql -h localhost -p 5432 -U Gr1zBear -d CoPos -c "SELECT current_database(), current_user;" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Подключение с пользователем Gr1zBear успешно!" -ForegroundColor Green
} else {
    Write-Host "ПРЕДУПРЕЖДЕНИЕ: Не удалось подключиться с пользователем Gr1zBear:" -ForegroundColor Yellow
    Write-Host $testUserConnection
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
Write-Host "Выполните: mvn spring-boot:run" -ForegroundColor Yellow

