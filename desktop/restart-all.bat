@echo off
setlocal
title Java10143 - restart backend + AI + H5
chcp 65001 >nul

rem ============================================================
rem  One-click restart of the three local services.
rem  ASCII ONLY - do not put Chinese characters in this file.
rem
rem  It launches them detached (own minimized windows), so they
rem  keep running after this window / the AI session is closed,
rem  and the phone on the same WiFi can reach them.
rem
rem    8080  Java10143 backend   (java -jar target\Java10143.jar)
rem    8010  Python AI service   (ai-service\server.py)
rem    5173  uni-app H5 dev srv  (HBuilderX node + uni.js, --host 0.0.0.0)
rem ============================================================

set "PROJ=E:\xiazai\huawei\Java10143"
set "JAVA=C:\Users\lenovo\.jdks\corretto-1.8.0_502\bin\java.exe"
set "PY=E:\xiazai\python.exe"
set "HBX=E:\Users\lenovo\Downloads\HBuilderX.5.26.2026091802\HBuilderX"
set "NODE=%HBX%\plugins\node\node.exe"
set "CLI=%HBX%\plugins\uniapp-cli-vite"

if not exist "%JAVA%" (
  echo [ERROR] java.exe not found: %JAVA%
  pause
  exit /b 1
)

echo [1/4] stopping old instances on 8080 / 8010 / 5173 ...
for %%P in (8080 8010 5173) do (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%P " ^| findstr "LISTENING"') do (
    echo       port %%P  ->  kill PID %%a
    taskkill /PID %%a /F >nul 2>&1
  )
)
timeout /t 2 /nobreak >nul

echo [2/4] starting backend on 8080 ...
start "Java10143Server" /min /d "%PROJ%" "%JAVA%" -Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m -jar "target\Java10143.jar" --server.port=8080

if exist "%PY%" (
  echo [3/4] starting AI service on 8010 ...
  start "Java10143AI" /min "%PY%" "%PROJ%\ai-service\server.py" 8010
) else (
  echo [3/4] [WARN] python not found: %PY%  - AI falls back to simple replies
)

if exist "%NODE%" (
  echo [4/4] starting H5 dev server on 5173 ...
  set "UNI_INPUT_DIR=%PROJ%\uni-app"
  set "UNI_OUTPUT_DIR=%PROJ%\uni-app\unpackage\dist\dev\.uniapp.h5"
  set "UNI_PLATFORM=h5"
  set "NODE_ENV=development"
  cd /d "%CLI%"
  start "Java10143H5" /min "%NODE%" node_modules\@dcloudio\vite-plugin-uni\bin\uni.js --host 0.0.0.0 --port 5173
) else (
  echo [4/4] [WARN] HBuilderX node not found: %NODE%
)

echo.
echo ============================================================
echo   done.
echo   PC    : http://localhost:5173
echo   Phone : http://192.168.100.200:5173   (WiFi)
echo   Phone : http://192.168.103.21:5173    (Ethernet)
echo   Backend: http://localhost:8080
echo ============================================================
timeout /t 8 /nobreak >nul
