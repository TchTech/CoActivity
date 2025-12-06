@echo off
REM Скрипт для очистки базы данных CoPos
REM Использование: clear_database.bat

echo ========================================
echo Очистка базы данных CoPos
echo ========================================
echo.
echo ВНИМАНИЕ: Этот скрипт удалит ВСЕ данные из базы данных!
echo.
set /p confirm="Вы уверены? (yes/no): "

if /i not "%confirm%"=="yes" (
    echo Операция отменена.
    pause
    exit /b
)

echo.
echo Подключение к базе данных...
echo.

REM Проверяем наличие psql
where psql >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ОШИБКА: psql не найден в PATH.
    echo Установите PostgreSQL или добавьте его в PATH.
    echo.
    echo Альтернативный способ: выполните скрипт вручную через psql:
    echo psql -h localhost -p 5433 -U Gr1zBear -d CoPos -f src\main\resources\sql\clear_database.sql
    pause
    exit /b 1
)

REM Выполняем SQL скрипт
psql -h localhost -p 5433 -U Gr1zBear -d CoPos -f src\main\resources\sql\clear_database.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo База данных успешно очищена!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ОШИБКА при очистке базы данных!
    echo ========================================
)

pause

