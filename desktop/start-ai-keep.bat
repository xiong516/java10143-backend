@echo off
rem ============================================================
rem  Java10143 Python AI service - keep running in own window
rem  Port 8010 (AI chat + video compose). ASCII ONLY.
rem ============================================================
title Java10143 AI 8010 (close this window to stop)

set "PROJ=E:\xiazai\huawei\Java10143"
set "PY=E:\xiazai\python.exe"
if not exist "%PY%" set "PY=python"

cd /d "%PROJ%\ai-service"

rem stop an old instance holding 8010
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8010" ^| findstr "LISTENING"') do (
  taskkill /PID %%a /F >nul 2>&1
)

echo ============================================================
echo   Java10143 AI service starting on port 8010
echo ============================================================
echo.

"%PY%" server.py 8010

echo.
echo [AI stopped] press any key to close this window
pause >nul
