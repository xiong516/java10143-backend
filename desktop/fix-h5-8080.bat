@echo off
setlocal
chcp 65001 >nul
title Java10143 - fix H5 on 8080  (RIGHT-CLICK -> Run as administrator)

rem ============================================================
rem  Why: the running backend serves the H5 from INSIDE
rem  target\Java10143.jar, so rebuilding the H5 or editing
rem  src/main/resources/static did NOT change what the phone saw.
rem  This script:
rem    1) stops the backend on 8080
rem    2) updates the H5 files inside the jar
rem    3) restarts the backend serving static from the disk folder
rem  Run it as administrator (the old backend may be elevated).
rem ============================================================

set "PROJ=E:\xiazai\huawei\Java10143"
set "JAVA=C:\Users\lenovo\.jdks\corretto-1.8.0_502\bin\java.exe"
set "JARBIN=C:\Users\lenovo\.jdks\corretto-1.8.0_502\bin"
if not exist "%JAVA%" set "JAVA=java"

echo [1/3] stop backend on 8080 ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
  echo       kill PID %%a
  taskkill /PID %%a /F >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo [2/3] update H5 files inside Java10143.jar ...
if exist "%JARBIN%\jar.exe" (
  cd /d "%PROJ%\src\main\resources"
  "%JARBIN%\jar.exe" uf "%PROJ%\target\Java10143.jar" static
  if errorlevel 1 echo       [WARN] jar update failed - will serve from disk folder anyway
) else (
  echo       [WARN] jar.exe not found - will serve from disk folder
)

echo [3/3] start backend on 8080 ...
cd /d "%PROJ%"
"%JAVA%" -Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m -jar "target\Java10143.jar" --server.port=8080 --spring.web.resources.static-locations=file:E:/xiazai/huawei/Java10143/src/main/resources/static/

echo.
echo [backend stopped] press any key to close this window
pause >nul
