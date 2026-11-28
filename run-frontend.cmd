@echo off
setlocal
rem ---------------------------------------------------------------
rem SwiftDrop - Terminal 2
rem Runs the React frontend with Vite hot reload.
rem ---------------------------------------------------------------
cd /d "%~dp0frontend"

if not exist "node_modules" (
  echo [1/2] Installing dependencies. First run, this takes a few minutes...
  call npm install
  if errorlevel 1 (
    echo [ERROR] npm install failed.
    exit /b 1
  )
) else (
  echo [1/2] Dependencies already installed.
)

rem No frontend\.env on purpose: api.ts defaults to the relative /api/v1 so
rem vite.config.ts can proxy /api/v1/notifications to :8081 and /api to :8080.

echo [2/2] Starting Vite dev server...
echo       App  http://localhost:5173
echo       Proxying to core-api :8080 and notification-service :8081
echo       Press Ctrl+C to stop.
echo.
call npm run dev
