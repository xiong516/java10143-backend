@echo off
rem Fix network adapter priority so HBuilderX picks the WiFi adapter (192.168.100.200)
rem instead of the VirtualBox virtual adapter (192.168.56.1).
rem Run by double-click. UAC prompt will appear - click Yes.

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Requesting administrator privileges...
    powershell -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo Setting WLAN metric to 1 (highest priority)...
powershell -Command "Set-NetIPInterface -InterfaceAlias 'WLAN' -AddressFamily IPv4 -InterfaceMetric 1"
echo Setting VirtualBox Host-Only metric to 100 (lowest priority)...
powershell -Command "Set-NetIPInterface -InterfaceAlias 'VirtualBox Host-Only Network' -AddressFamily IPv4 -InterfaceMetric 100"

echo.
echo Done. Current priority:
powershell -Command "Get-NetIPInterface -AddressFamily IPv4 | Sort-Object InterfaceMetric | Select-Object InterfaceAlias,InterfaceMetric -First 6 | Format-Table -AutoSize"
pause
