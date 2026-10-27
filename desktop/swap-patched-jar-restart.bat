@echo off
setlocal EnableDelayedExpansion
title JD10143 swap patched jar + restart backend 8080
rem ============================================================
rem  Swap target\Java10143.patched.jar into target\Java10143.jar
rem  and restart ONLY the 8080 backend. AI(8010) and H5 dev(5173)
rem  keep running untouched.
rem
rem  Use this when "rebuild jar and full restart" bat fails, or
rem  when you want the patched static assets live without a full
rem  maven rebuild. The patched jar was verified byte-for-byte
rem  against src\main\resources\static before staging.
rem
rem  ASCII ONLY - do not put Chinese characters in this file.
rem ============================================================

rem --- auto-elevate (needed to kill the elevated backend) ---
NET SESSION >nul 2>&1
if %errorlevel% neq 0 (
  powershell -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

set "PROJ=E:\xiazai\huawei\Java10143"
set "JAVA=C:\Users\lenovo\.jdks\corretto-1.8.0_502\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"
set "PORT=8080"

if not exist "%PROJ%\target\Java10143.patched.jar" (
  echo [ERROR] patched jar not found.
  echo         expected: %PROJ%\target\Java10143.patched.jar
  pause
  exit /b 1
)

echo [1/4] stopping backend on %PORT% ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr "LISTENING"') do (
  echo       kill PID %%a
  taskkill /PID %%a /F >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo [2/4] backing up current jar ...
copy /Y "%PROJ%\target\Java10143.jar" "%PROJ%\target\Java10143.jar.bak_before_swap" >nul

echo [3/4] swapping in patched jar ...
copy /Y "%PROJ%\target\Java10143.patched.jar" "%PROJ%\target\Java10143.jar" >nul
if errorlevel 1 (
  echo [ERROR] copy failed - is the jar still locked? try again in a few seconds.
  pause
  exit /b 1
)
echo       swapped OK ^(backup: target\Java10143.jar.bak_before_swap^)

echo [4/4] starting backend on %PORT% ...
start "JD10143-Backend" /min /d "%PROJ%" "%JAVA%" -Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m -jar "target\Java10143.jar" --server.port=%PORT%

set "CURL=%SystemRoot%\System32\curl.exe"
if not exist "%CURL%" set "CURL=curl"
set "PROBE=%TEMP%\jd_swap_probe.txt"
set /a tries=0
:waitloop
timeout /t 2 /nobreak >nul
del "%PROBE%" >nul 2>&1
"%CURL%" -s --noproxy "*" --max-time 3 -o "%PROBE%" "http://127.0.0.1:%PORT%/home/banner" >nul 2>&1
if exist "%PROBE%" findstr /c:"\"code\"" "%PROBE%" >nul 2>&1 && goto backend_up
set /a tries+=1
if !tries! lss 45 goto waitloop
echo       [WARN] api not ready after 90s - check the JD10143-Backend window.
goto done
:backend_up
del "%PROBE%" >nul 2>&1
echo       backend is UP.
:done
echo.
echo   done. patched assets now live on http://localhost:%PORT%
echo   PC browser : http://localhost:8080
echo   Phone      : http://192.168.100.200:8080
echo   H5 dev     : http://localhost:5173 ^(untouched, already latest^)
echo.
pause
