@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title Java10143 Shop  (close this window to stop the server)

rem ============================================================
rem  Java10143 Shop - start backend (JDK 8) + open the web UI
rem  Port : 8080 (keep it 8080 - web / harmony app / mini program
rem         all point to http://<your-ip>:8080)
rem ============================================================

cd /d "%~dp0.."
echo.
echo ============================================================
echo   Java10143 Shop  -  starting backend + opening the web UI
echo ============================================================
echo   Project : %CD%
echo.

rem ---------- 1. locate JDK (JDK 8 or newer, JDK 8 preferred) ----------
call "%~dp0find-java.bat" 2>nul
if not defined JDK_HOME (
  echo [ERROR] JDK 8+ not found.
  echo         Please install JDK 8, or set JAVA_HOME to your JDK folder.
  echo         Tried: DevEco JBR, %%USERPROFILE%%\.jdks\corretto-1.8.0_502,
  echo                %%JAVA_HOME%%, java on PATH
  pause
  exit /b 1
)
set "JAVA_EXE=%JDK_HOME%\bin\java.exe"
echo [OK] Java : %JDK_HOME%

rem ---------- 2. jar ----------
if not exist "target\Java10143.jar" (
  echo [WARN] target\Java10143.jar not found, building first...
  call "%~dp0build-java10143.bat"
  if not exist "target\Java10143.jar" (
    echo [ERROR] Build failed, cannot start.
    pause
    exit /b 1
  )
)

rem ---------- 3. already running? ----------
netstat -ano | findstr /r /c:"TCP.*:8080 .*LISTENING" >nul
if not errorlevel 1 (
  echo [OK] Port 8080 is already in use - server seems to be running.
  goto :AI
)

rem ---------- 4. start ----------
echo [..] Starting server on http://localhost:8080
start "Java10143Server" /min "%JAVA_EXE%" -Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m -jar "target\Java10143.jar" --server.port=8080

echo [..] Waiting for the server to come up...
set /a COUNT=0
:WAIT
set /a COUNT+=1
timeout /t 1 /nobreak >nul
netstat -ano | findstr /r /c:"TCP.*:8080 .*LISTENING" >nul
if not errorlevel 1 goto :OPEN
if %COUNT% lss 40 goto :WAIT
echo [WARN] Server did not answer in 40s, opening the browser anyway.

rem ---------- 4.5 AI service (optional, start silently if python exists) ----------
:AI
netstat -ano | findstr /r /c:"TCP.*:8010 .*LISTENING" >nul
if not errorlevel 1 (
  echo [OK] AI service already running on port 8010.
  goto :OPEN
)

set "PY="
where python >nul 2>nul && set "PY=python"
if not defined PY (
  if exist "%USERPROFILE%\.workbuddy\binaries\python\versions\3.13.12\python.exe" (
    set "PY=%USERPROFILE%\.workbuddy\binaries\python\versions\3.13.12\python.exe"
  )
)
if not defined PY (
  if exist "C:\Users\lenovo\.workbuddy\binaries\python\versions\3.13.12\python.exe" (
    set "PY=C:\Users\lenovo\.workbuddy\binaries\python\versions\3.13.12\python.exe"
  )
)

if not defined PY (
  echo [WARN] Python not found - AI chat will use the simple fallback replies.
  goto :OPEN
)

echo [..] Starting AI service on http://localhost:8010
start "Java10143 AI" /min "%PY%" "%~dp0..\ai-service\server.py" 8010
echo [OK] AI service started (close its window to stop the AI chat).

:OPEN
echo [OK] Opening the shop UI ...
if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" (
  start "" "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" --app=http://localhost:8080
) else if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" (
  start "" "%ProgramFiles%\Google\Chrome\Application\chrome.exe" --app=http://localhost:8080
) else if exist "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" (
  start "" "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" --app=http://localhost:8080
) else (
  start "" http://localhost:8080
)

echo.
echo   Web UI  : http://localhost:8080
echo   Login   : hm / test / admin    (password: 123456)
echo   AI chat : run ai-service\start-ai.bat first (port 8010)
echo.
echo   Keep this window open while using the app.
echo.
pause
