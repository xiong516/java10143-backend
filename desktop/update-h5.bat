@echo off
rem ============================================================
rem  Java10143 - rebuild uni-app H5 into the backend jar
rem
rem  USE CASE: you edited uni-app source (pages / api/request.js
rem  etc.) and want the WEB production build updated. This one
rem  script does the whole chain:
rem
rem    [1] uni.js build -p h5      (HBuilderX cli, production)
rem    [2] copy dist -> static     (assets + index.html)
rem    [3] kill the 8080 backend   (jar is locked while running)
rem    [4] mvn package             (corretto jdk8, no clean)
rem    [5] remind: double click "jing dong quan mian ban" to start
rem
rem  NOTE: uni-app H5 production uses SAME-ORIGIN api base, so
rem  changing tunnel domains does NOT need this script - only
rem  source code changes do.
rem
rem  ASCII ONLY. Port rule: JD = 8080, coffee = 8082.
rem ============================================================
setlocal EnableDelayedExpansion
title Java10143 H5 rebuild + package

set "PROJ=E:\xiazai\huawei\Java10143"
set "UNI=%PROJ%\uni-app"
set "STATIC=%PROJ%\src\main\resources\static"
set "HBX=E:\Users\lenovo\Downloads\HBuilderX.5.26.2026091802\HBuilderX"
set "NODE=%HBX%\plugins\node\node.exe"
set "CLI=%HBX%\plugins\uniapp-cli-vite"
set "JAVA_HOME=C:\Users\lenovo\.jdks\corretto-1.8.0_502"
set "MVN=C:\Users\lenovo\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd"

echo ============================================================
echo   Java10143  -  H5 rebuild + jar package
echo ============================================================
echo.

if not exist "%NODE%" (
  echo [ERROR] HBuilderX node not found: %NODE%
  pause
  exit /b 1
)

rem ---------- [1] build ----------
echo [1/5] building uni-app H5 production bundle ...
set "UNI_INPUT_DIR=%UNI%"
set "UNI_PLATFORM=h5"
set "NODE_ENV=production"
cd /d "%CLI%"
call "%NODE%" node_modules\@dcloudio\vite-plugin-uni\bin\uni.js build -p h5
if errorlevel 1 (
  echo [ERROR] uni build failed - see errors above.
  pause
  exit /b 1
)

rem locate the build output (h5 folder)
set "OUT=%UNI%\unpackage\dist\build\h5"
if not exist "%OUT%\index.html" (
  echo [ERROR] build output not found: %OUT%\index.html
  pause
  exit /b 1
)

rem ---------- [2] deploy to static ----------
echo [2/5] deploying build output into static ...
if exist "%STATIC%\assets" rmdir /s /q "%STATIC%\assets"
if exist "%STATIC%\index.html" del /q "%STATIC%\index.html"
xcopy /E /I /Y "%OUT%\assets" "%STATIC%\assets\" >nul
copy /Y "%OUT%\index.html" "%STATIC%\index.html" >nul
if errorlevel 1 (
  echo [ERROR] copy to static failed.
  pause
  exit /b 1
)
echo       deployed: %STATIC%

rem ---------- [3] stop backend ----------
echo [3/5] stopping the 8080 backend ^(jar file must be unlocked^) ...
set "KILLED="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
  echo       kill PID %%a
  taskkill /PID %%a /F >nul 2>&1
  set "KILLED=1"
)
if not defined KILLED echo       backend was not running.
timeout /t 2 /nobreak >nul

rem ---------- [4] package ----------
echo [4/5] re-packing jar ^(mvn package, no clean^) ...
cd /d "%PROJ%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%MVN%" -B -q -DskipTests package
if errorlevel 1 (
  echo [ERROR] mvn package failed - see errors above.
  echo         make sure no window is holding target\Java10143.jar
  pause
  exit /b 1
)
echo       jar updated: %PROJ%\target\Java10143.jar

echo [5/5] done.

echo.
echo ============================================================
echo   ALL DONE.
echo   Now double click the desktop "jing dong quan mian ban" bat
echo   to start the backend + AI + H5 dev server again.
echo.
echo   PC    : http://localhost:8080
echo   Phone : http://192.168.100.200:8080   ^(WiFi^)
echo   Phone : http://192.168.103.21:8080    ^(Ethernet^)
echo ============================================================
pause
