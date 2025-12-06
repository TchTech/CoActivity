@echo off
echo ========================================
echo Очистка базы данных CoPos
echo ========================================
echo.

REM Компилируем проект
echo Компиляция проекта...
call mvn compile -q
if %ERRORLEVEL% NEQ 0 (
    echo Ошибка компиляции!
    pause
    exit /b 1
)

REM Запускаем очистку
echo Запуск очистки базы данных...
call mvn exec:java -Dexec.mainClass="com.mipt.CoActivity.util.StandaloneDatabaseCleaner" -Dexec.classpathScope="compile"

echo.
pause

