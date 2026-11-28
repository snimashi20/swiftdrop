@echo off
setlocal
rem ---------------------------------------------------------------
rem SwiftDrop - Terminal 1
rem Runs core-api on the host against the Dockerised infrastructure.
rem ---------------------------------------------------------------
cd /d "%~dp0"

if not exist ".env" (
  echo [ERROR] .env not found in %cd%
  echo         Copy .env.example to .env and fill it in first.
  exit /b 1
)

echo [1/4] Starting infrastructure containers...
docker compose up -d --no-deps postgres rabbitmq mailpit notification-service ai-assistant
if errorlevel 1 (
  echo [ERROR] docker compose failed. Is Docker Desktop running?
  exit /b 1
)

echo [2/4] Loading .env into this session...
for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do set "%%A=%%B"

rem .env sets CORS_ALLOWED_ORIGINS twice and the last one drops :5173,
rem which would block the Vite dev server. Force both origins.
set "CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173"

rem .env also sets FRONTEND_BASE_URL=:3000, the Docker/nginx port. Here the
rem frontend is Vite on :5173, so the OAuth success redirect must point there
rem or Google login would land on a dead port.
set "FRONTEND_BASE_URL=http://localhost:5173"
rem Not the 'dev' profile: dev uses in-memory H2, and the containerised
rem notification-service reads the real Postgres.
set "SPRING_PROFILES_ACTIVE=docker"

echo [3/4] Selecting JDK 21...
if exist "C:\Program Files\Java\jdk-21.0.12.1\bin\javac.exe" (
  set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1"
) else (
  echo [WARN] jdk-21.0.12.1 not found, falling back to %JAVA_HOME%
  echo [WARN] The build needs JDK 21. JDK 17 fails with "release version 21 not supported".
)

echo [4/4] Starting core-api...
echo       API     http://localhost:8080
echo       Swagger http://localhost:8080/swagger-ui.html
echo       Mailpit http://localhost:8025
echo       Press Ctrl+C to stop.
echo.
cd backend\core-api
call mvnw.cmd spring-boot:run
