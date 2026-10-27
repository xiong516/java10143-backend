@echo off
chcp 65001 >nul
title Java10143 - stop
echo Stopping Java10143 server (port 8080) ...

set "FOUND="
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:"TCP.*:8080 .*LISTENING"') do (
  set "FOUND=1"
  echo   killing PID %%p
  taskkill /f /pid %%p >nul 2>nul
)

if not defined FOUND (
  echo   nothing is listening on port 8080.
) else (
  echo [OK] server stopped.
)
pause
