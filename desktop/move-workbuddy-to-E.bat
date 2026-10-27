@echo off
REM ============================================================
REM  Move WorkBuddy data folder (.workbuddy) to E: drive
REM  Safe method:  copy  ->  rename original  ->  junction  -> verify
REM  After that WorkBuddy still uses C:\Users\<you>\.workbuddy
REM  (it is a link), so nothing breaks and no history is lost.
REM  MUST close WorkBuddy completely before running this.
REM ============================================================
setlocal enabledelayedexpansion
title Move WorkBuddy data to E

set "SRC=C:\Users\lenovo\.workbuddy"
set "DST=E:\WorkBuddyData\.workbuddy"
set "OLD=%SRC%_old"

echo ============================================================
echo   WorkBuddy data migration  (C: --^> E:)
echo ============================================================
echo   Source : %SRC%
echo   Target : %DST%
echo.

REM ---------- 1. make sure WorkBuddy is closed ----------
tasklist /FI "IMAGENAME eq WorkBuddy.exe" 2>nul | find /I "WorkBuddy.exe" >nul
if not errorlevel 1 goto running
tasklist /FI "IMAGENAME eq WorkBuddyHelper.exe" 2>nul | find /I "WorkBuddyHelper.exe" >nul
if not errorlevel 1 goto running
goto checksrc
:running
echo [X] WorkBuddy is still running.
echo     Please quit WorkBuddy from the tray icon first, then run again.
echo.
pause
exit /b 1

:checksrc
if not exist "%SRC%" (
    echo [-] Source folder not found. Nothing to do.
    pause & exit /b 0
)
if exist "%SRC%\*" goto docopy
echo [-] Source folder is empty. Nothing to do.
pause & exit /b 0

:docopy
if not exist "E:\WorkBuddyData" mkdir "E:\WorkBuddyData"
echo [1/5] Copying data to E: (this may take a few minutes, 5GB+) ...
echo       Please do not close this window.
robocopy "%SRC%" "%DST%" /E /R:1 /W:1 /NFL /NDL /NJH /NJS /NP
if errorlevel 8 (
    echo.
    echo [X] Copy failed ^(robocopy errorlevel %errorlevel%^).
    echo     Nothing was changed. Your original data is untouched.
    pause & exit /b 1
)

echo.
echo [2/5] Renaming original folder to %OLD% ...
if exist "%OLD%" rmdir "%OLD%" /s /q >nul 2>&1
move "%SRC%" "%OLD%" >nul
if errorlevel 1 (
    echo [X] Cannot rename the original folder. Close every WorkBuddy process and retry.
    pause & exit /b 1
)

echo [3/5] Creating junction ...
mklink /J "%SRC%" "%DST%" >nul
if not exist "%SRC%\settings.json" (
    echo [X] Junction check failed. Rolling back ...
    if exist "%SRC%" rmdir "%SRC%" /s /q >nul 2>&1
    move "%OLD%" "%SRC%" >nul
    echo     Rollback done. Original data restored at %SRC%
    pause & exit /b 1
)

echo [4/5] Verify ...
if not exist "%SRC%\webui.url" if not exist "%SRC%\projects" (
    echo [X] Verification failed. Rolling back ...
    if exist "%SRC%" rmdir "%SRC%" /s /q >nul 2>&1
    move "%OLD%" "%SRC%" >nul
    pause & exit /b 1
)
echo     OK - WorkBuddy will use the E: copy through the link.

echo.
echo [5/5] Old copy is kept at:
echo       %OLD%
echo     It takes about the same space. Delete it only after you
echo     start WorkBuddy once and everything looks normal.
echo.
choice /C YN /M "Delete the old copy now to free C: space"
if errorlevel 2 goto keepold
rmdir "%OLD%" /s /q
echo     Old copy deleted.
goto done
:keepold
echo     Kept. Delete this folder later manually: %OLD%
:done
echo.
echo ============================================================
echo   Done. Start WorkBuddy now and check your history.
echo   If anything looks wrong: delete the junction folder
echo   %SRC%  (right click - delete, it only removes the link,
echo   the real data stays on E:), then rename %OLD% back.
echo ============================================================
pause
exit /b 0
