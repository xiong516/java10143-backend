@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title Java10143 - Phone Hotspot Mode

rem ============================================================
rem  Java10143 - phone hotspot / LAN mode launcher
rem
rem  WHY: the phone cannot open the cpolar tunnel over 4G
rem       (carrier blocks or hijacks *.cpolar.top). In hotspot
rem       mode the laptop joins the PHONE's hotspot, so the phone
rem       opens the laptop ip directly - the traffic never leaves
rem       the local network, so nothing can intercept it.
rem
rem  WHAT IT DOES:
rem    1. detects the real LAN ip (skips VirtualBox / VMware /
rem       Hyper-V virtual adapters - see get_lan_ip.ps1)
rem    2. starts the backend on 8080 if it is not running
rem    3. starts the python AI service on 8010 if not running
rem    4. opens the page locally so you can verify it works
rem    5. saves the url to desktop\phone-url.txt
rem
rem  ASCII ONLY - do not put Chinese characters in this file.
rem ============================================================

set "PROJ=E:\xiazai\huawei\Java10143"
set "JAVA=C:\Users\lenovo\.jdks\corretto-1.8.0_502\bin\java.exe"
set "PY=E:\xiazai\python.exe"
set "PORT=8080"
set "IPSCRIPT=%PROJ%\desktop\get_lan_ip.ps1"

echo ============================================================
echo   Java10143  -  Phone hotspot / LAN mode
echo ============================================================
echo.

if not exist "%JAVA%" (
  echo [ERROR] java.exe not found:
  echo         %JAVA%
  echo.
  pause
  exit /b 1
)

rem ------------------------------------------------------------
rem [1/5] detect the real LAN ip
rem ------------------------------------------------------------
echo [1/5] detecting this PC ip on the real network card ...

set "MYIP="
for /f "usebackq delims=" %%a in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%IPSCRIPT%"`) do (
  if not defined MYIP set "MYIP=%%a"
)

if not defined MYIP (
  echo.
  echo [ERROR] could not detect a LAN ip on a physical adapter.
  echo         Connect the PC to wifi or to the phone hotspot first.
  echo.
  echo --- adapters seen ---
  powershell -NoProfile -ExecutionPolicy Bypass -File "%IPSCRIPT%" -All
  echo.
  pause
  exit /b 1
)

echo       PC ip : !MYIP!
echo.

rem ------------------------------------------------------------
rem [2/5] backend on 8080
rem ------------------------------------------------------------
echo [2/5] checking backend on port %PORT% ...

set "RUNNING="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr "LISTENING"') do set "RUNNING=1"

if defined RUNNING (
  echo       already running - reusing it.
) else (
  echo       not running - starting it now ...
  start "Java10143Server" /min /d "%PROJ%" "%JAVA%" -Xmx512m -XX:MaxMetaspaceSize=256m -jar "target\Java10143.jar" --server.port=%PORT%

  echo       waiting for it to come up ...
  set "OK="
  for /l %%i in (1,1,60) do (
    if not defined OK (
      timeout /t 1 /nobreak >nul
      for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr "LISTENING"') do set "OK=1"
    )
  )
  if defined OK (
    echo       backend is up.
  ) else (
    echo       [WARN] backend not up after 60s - check the
    echo              minimized "Java10143Server" window.
  )
)

rem ------------------------------------------------------------
rem [3/5] python AI service on 8010
rem ------------------------------------------------------------
echo [3/5] checking AI service on port 8010 ...
set "AIRUN="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8010 " ^| findstr "LISTENING"') do set "AIRUN=1"
if defined AIRUN (
  echo       already running.
) else (
  if exist "%PY%" (
    echo       starting it ...
    start "Java10143AI" /min "%PY%" "%PROJ%\ai-service\server.py" 8010
  ) else (
    echo       [WARN] python not found - AI uses fallback replies.
  )
)

rem ------------------------------------------------------------
rem [4/5] url card
rem ------------------------------------------------------------
echo [4/5] writing url card ...
set "CARD=%PROJ%\desktop\phone-url.txt"
(
  echo Java10143 - phone access url
  echo generated %DATE% %TIME%
  echo.
  echo   http://!MYIP!:%PORT%
  echo.
  echo mall    http://!MYIP!:%PORT%/
  echo video   http://!MYIP!:%PORT%/video.html
  echo admin   http://!MYIP!:%PORT%/admin.html
) > "%CARD%" 2>nul
if exist "%CARD%" echo       saved: %CARD%

rem ------------------------------------------------------------
rem [5/5] open locally to verify
rem ------------------------------------------------------------
echo [5/5] opening the page on this PC to verify ...
start "" "http://!MYIP!:%PORT%"

echo.
echo ============================================================
echo   OPEN THIS ON YOUR PHONE
echo ============================================================
echo.
echo        http://!MYIP!:%PORT%
echo.
echo   mall    http://!MYIP!:%PORT%/
echo   video   http://!MYIP!:%PORT%/video.html
echo   admin   http://!MYIP!:%PORT%/admin.html
echo.
echo ============================================================
echo   STEPS
echo ============================================================
echo.
echo   1. Phone : turn on mobile data AND personal hotspot.
echo   2. Laptop: connect its wifi to the PHONE hotspot.
echo   3. Re-run this script after the network switch
echo      (the ip changes between wifi and hotspot).
echo   4. Phone : open the url above in the browser.
echo.
echo   If the phone still cannot open it:
echo     - some hotspot "guest" modes block client-to-client
echo       traffic, use the normal hotspot instead
echo     - allow java.exe when the Windows Firewall prompt pops
echo.
echo ============================================================
echo   DONE
echo ============================================================
echo.
pause
