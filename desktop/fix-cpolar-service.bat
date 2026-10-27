@echo off
REM Fix cpolar tunnel: start cpolar Windows service (auto-elevate)
REM ASCII only - no Chinese in this file (encoding safety)

NET SESSION >nul 2>&1
if %errorlevel% neq 0 (
    powershell -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo [1/3] Stopping orphan standalone cpolar.exe (not the service)...
taskkill /f /im cpolar.exe >nul 2>&1

echo [2/3] Starting cpolar service...
net start cpolar
if %errorlevel% neq 0 (
    echo Failed to start service. Try: restart PC or check cpolar install.
    pause
    exit /b 1
)

echo [3/3] Waiting for tunnel to establish (poll every 2s, max 40s)...
set /a TRIES=0
:POLL
set /a TRIES+=1
netstat -ano | findstr ":4040 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo Tunnel is UP. Checks used: %TRIES%
    goto DONE
)
if %TRIES% geq 20 (
    echo Tunnel not detected after 40s. Open cpolar Web UI to check manually.
    goto DONE
)
timeout /t 2 /nobreak >nul
goto POLL

:DONE
echo.
echo Done. Now double-click the desktop shortcut to update the tunnel address:
echo   UpdateTunnelAddress.bat
echo.
pause
