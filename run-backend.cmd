@echo off
cd /d "%~dp0"
if exist "backend\config\database.cmd" call "backend\config\database.cmd"
if defined STUDYPLANNER_DB_CONTAINER goto saved_database
docker --context desktop-linux compose up -d --wait db
goto database_checked
:saved_database
docker --context desktop-linux start "%STUDYPLANNER_DB_CONTAINER%"
:database_checked
if errorlevel 1 (
  echo PostgreSQL could not start. Open Docker Desktop and try again.
  exit /b 1
)
cd backend
call mvnw.cmd -B -ntp spring-boot:run
