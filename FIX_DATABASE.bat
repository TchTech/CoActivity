@echo off
echo ========================================
echo Настройка PostgreSQL для CoActivity
echo ========================================
echo.

REM Добавляем PostgreSQL в PATH
set PATH=%PATH%;C:\Program Files\PostgreSQL\18\bin
set PATH=%PATH%;C:\Program Files\PostgreSQL\17\bin
set PATH=%PATH%;C:\Program Files\PostgreSQL\16\bin

echo Введите пароль пользователя postgres:
echo (Обычно это: postgres или ваш пароль Windows)
echo.
set /p POSTGRES_PASSWORD="Пароль: "

if "%POSTGRES_PASSWORD%"=="" (
    set PGPASSWORD=
) else (
    set PGPASSWORD=%POSTGRES_PASSWORD%
)

echo.
echo Попытка подключения к PostgreSQL...
psql -h localhost -p 5432 -U postgres -d postgres -c "SELECT version();" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ОШИБКА: Не удалось подключиться к PostgreSQL
    echo Проверьте пароль и что PostgreSQL запущен
    pause
    exit /b 1
)

echo Подключение успешно!
echo.

echo Создание пользователя Gr1zBear...
psql -h localhost -p 5432 -U postgres -d postgres -c "DROP USER IF EXISTS \"Gr1zBear\";" >nul 2>&1
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE USER \"Gr1zBear\" WITH PASSWORD 'qwerty';" 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ОШИБКА при создании пользователя
    pause
    exit /b 1
)
echo Пользователь создан!
echo.

echo Создание базы данных CoPos...
psql -h localhost -p 5432 -U postgres -d postgres -c "DROP DATABASE IF EXISTS \"CoPos\";" >nul 2>&1
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE DATABASE \"CoPos\" OWNER \"Gr1zBear\";" 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ОШИБКА при создании базы данных
    pause
    exit /b 1
)
echo База данных создана!
echo.

echo Выдача привилегий...
psql -h localhost -p 5432 -U postgres -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE \"CoPos\" TO \"Gr1zBear\";" >nul 2>&1

set PGPASSWORD=qwerty
echo.
echo Проверка подключения с новым пользователем...
psql -h localhost -p 5432 -U Gr1zBear -d CoPos -c "SELECT current_database(), current_user;" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Подключение успешно!
) else (
    echo Предупреждение: Не удалось подключиться с новым пользователем
)

echo.
echo ========================================
echo Настройка завершена!
echo ========================================
echo.
echo Параметры подключения:
echo   База данных: CoPos
echo   Пользователь: Gr1zBear
echo   Пароль: qwerty
echo   Порт: 5432
echo.
echo Теперь можно запускать Spring Boot приложение!
echo.
pause

