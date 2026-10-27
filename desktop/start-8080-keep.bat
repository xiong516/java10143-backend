@echo off
rem ============================================================
rem  Java10143 backend - keep running in its own window
rem  ASCII ONLY. Port 8080 (JD / 京东). Coffee is 8082 - do not touch.
rem
rem  Why: processes started from an AI/tool session get recycled when
rem  the session ends. Double-click this .bat instead - the window
rem  stays open (minimized) and the backend keeps serving.
rem ============================================================
title Java10143 backend 8080 (close this window to stop)

set "PROJ=E:\xiazai\huawei\Java10143"
set "JAVA_EXE=C:\Users\lenovo\.jdks\corretto-1.8.0_502\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

cd /d "%PROJ%"

rem stop an old instance holding 8080 (only the Java10143 one)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  taskkill /PID %%a /F >nul 2>&1
)

echo ============================================================
echo   Java10143 backend starting on port 8080
echo   Web: http://localhost:8080
echo ============================================================
echo.

"%JAVA_EXE%" -Xms128m -Xmx512m -jar "target\Java10143.jar" --server.port=8080

echo.
echo [backend stopped] press any key to close this window
pause >nul
