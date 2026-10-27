@echo off
rem ============================================================
rem  Free memory before HBuilderX packaging (APK build needs RAM)
rem  ASCII ONLY - do not put Chinese characters in this file.
rem
rem  Why: HBuilderX local packaging runs a Java toolchain
rem  (apk build + zipalign). If free RAM is too low you get:
rem    "There is insufficient memory for the Java Runtime..."
rem    "Apk zipalign failed"
rem
rem  This script closes the biggest memory hogs that are SAFE to
rem  close before packaging:
rem    - Android emulator (Emulator.exe / qemu-system / HD-Player)
rem    - WeChat mini program renderers (WeChatAppEx.exe)
rem  It does NOT touch: HBuilderX, java backends, MySQL, WeChat itself.
rem ============================================================
setlocal enabledelayedexpansion
chcp 65001 >nul
title Free memory for HBuilderX packaging

echo ============================================================
echo   Free memory before packaging
echo ============================================================
echo.

for /f "tokens=2 delims==" %%m in ('wmic OS get FreePhysicalMemory /value 2^>nul ^| find "="') do set FREE_BEFORE=%%m
set /a MB_BEFORE=%FREE_BEFORE%/1024
echo [before] free RAM = %MB_BEFORE% MB
echo.

echo Closing Android emulator ...
taskkill /F /IM Emulator.exe       >nul 2>&1 && echo    Emulator.exe        closed
taskkill /F /IM qemu-system-x86_64.exe >nul 2>&1 && echo    qemu-system-x86_64  closed
taskkill /F /IM qemu-system-aarch64.exe >nul 2>&1 && echo    qemu-system-aarch64 closed
taskkill /F /IM HD-Player.exe      >nul 2>&1 && echo    HD-Player.exe       closed
taskkill /F /IM Nox.exe            >nul 2>&1 && echo    Nox.exe             closed
taskkill /F /IM LdVBoxHeadless.exe >nul 2>&1 && echo    LdVBoxHeadless.exe  closed

echo Closing WeChat mini program renderers ...
taskkill /F /IM WeChatAppEx.exe    >nul 2>&1 && echo    WeChatAppEx.exe     closed

rem Let Windows reclaim the memory
timeout /t 3 /nobreak >nul

for /f "tokens=2 delims==" %%m in ('wmic OS get FreePhysicalMemory /value 2^>nul ^| find "="') do set FREE_AFTER=%%m
set /a MB_AFTER=%FREE_AFTER%/1024
echo.
echo ============================================================
echo   [after ] free RAM = %MB_AFTER% MB
if %MB_AFTER% GEQ 3000 (
  echo   OK - enough memory, you can package now.
) else (
  echo   WARNING - still low ^(< 3000 MB^).
  echo   Close more apps: browser tabs, WeChat, games.
  echo   Or use cloud packaging instead of local packaging.
)
echo ============================================================
echo.
echo Now go back to HBuilderX and click Package again.
echo.
pause
