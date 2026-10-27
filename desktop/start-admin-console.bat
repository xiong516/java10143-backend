@echo off
setlocal
title JD Admin 8080: backend + AI + browser
chcp 65001 >nul

rem ============================================================
rem  JD admin console launcher. ASCII ONLY.
rem  [1] kill old 8080/8010 listeners
rem  [2] start Java backend 8080 + Python AI 8010 (minimized)
rem  [3] wait until 8080 ready
rem  [4] open admin console in browser
rem  Port rule: JD = 8080. NEVER touches 8082 (coffee).
rem ============================================================

set "PROJ=E:\xiazai\huawei\Java10143"
set "JAVA=C:\Users\lenovo\.jdks\corretto-1.8.0_502\bin\java.exe"
set "PY=E:\xiazai\python.exe"
set "PORT=8080"

if not exist "%JAVA%" (
  echo [ERROR] java.exe not found: %JAVA%
  pause
  exit /b 1
)
if not exist "%PROJ%\target\Java10143.jar" (
  echo [ERROR] jar not found: %PROJ%\target\Java10143.jar
  echo         run desktop\build-java10143.bat first.
  pause
  exit /b 1
)

echo [1/4] stopping old instances on 8080 / 8010 ...
for %%P in (8080 8010) do (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%P " ^| findstr "LISTENING"') do (
    echo       port %%P  -^>  kill PID %%a
    taskkill /PID %%a /F >nul 2>&1
  )
)
timeout /t 2 /nobreak >nul

echo [2/4] starting Java backend %PORT% + Python AI 8010 (minimized) ...
start "JD-Backend" /min /d "%PROJ%" "%JAVA%" -Xms256m -Xmx768m -jar "target\Java10143.jar" --server.port=%PORT%
start "JD-AI-8010" /min /d "%PROJ%\ai-service" "%PY%" server.py 8010

echo [3/4] waiting for %PORT% ...
set /a tries=0
:WAIT
netstat -ano | findstr ":%PORT% " | findstr "LISTENING" >nul
if not errorlevel 1 goto READY
set /a tries+=1
if %tries% geq 40 (
  echo       [WARN] backend not up after 60s. Check the JD-Backend window.
  goto READY
)
timeout /t 2 /nobreak >nul
goto WAIT
:READY
echo       backend is UP.

echo [4/4] opening admin console ...
start "" "http://localhost:%PORT%/admin.html"

echo.
echo ============================================================
echo   Admin : http://localhost:8080/admin.html   (login: admin)
echo   Video : http://localhost:8080/video.html
echo   Note  : keep the two minimized windows (JD-Backend /
echo           JD-AI-8010) open, closing them stops the services.
echo ============================================================
timeout /t 10 /nobreak >nul
