@echo off
chcp 65001 >nul
echo ========================================
echo Creating PostgreSQL User and Database
echo ========================================
echo.

REM Add PostgreSQL to PATH
set "PGPATH=C:\Program Files\PostgreSQL\18\bin"
if not exist "%PGPATH%\psql.exe" (
    set "PGPATH=C:\Program Files\PostgreSQL\17\bin"
)
if not exist "%PGPATH%\psql.exe" (
    set "PGPATH=C:\Program Files\PostgreSQL\16\bin"
)
set "PATH=%PATH%;%PGPATH%"

echo Attempting to connect as postgres user...
echo Please enter postgres password when prompted.
echo.

REM Try to create user (will prompt for password)
echo Step 1: Creating user Gr1zBear...
psql -h localhost -p 5432 -U postgres -d postgres -c "DROP USER IF EXISTS \"Gr1zBear\";"
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE USER \"Gr1zBear\" WITH PASSWORD 'qwerty';"

if %ERRORLEVEL% EQU 0 (
    echo User Gr1zBear created successfully!
) else (
    echo Failed to create user. Please check the password.
    pause
    exit /b 1
)

echo.
echo Step 2: Creating database CoPos...
psql -h localhost -p 5432 -U postgres -d postgres -c "DROP DATABASE IF EXISTS \"CoPos\";"
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE DATABASE \"CoPos\" OWNER \"Gr1zBear\";"

if %ERRORLEVEL% EQU 0 (
    echo Database CoPos created successfully!
) else (
    echo Failed to create database.
    pause
    exit /b 1
)

echo.
echo Step 3: Granting privileges...
psql -h localhost -p 5432 -U postgres -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE \"CoPos\" TO \"Gr1zBear\";"

echo.
echo Step 4: Testing connection...
set PGPASSWORD=qwerty
psql -h localhost -p 5432 -U Gr1zBear -d CoPos -c "SELECT current_database(), current_user;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! User and database created.
    echo You can now start Spring Boot application.
    echo ========================================
) else (
    echo Warning: Could not test connection with new user.
)

pause

