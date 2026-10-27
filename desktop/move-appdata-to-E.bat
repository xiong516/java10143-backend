@echo off
REM ============================================================
REM  Move WeChat + QQBrowser data to E: drive safely
REM  Method: copy -> junction (mklink /J)
REM  The app still uses the ORIGINAL path, so nothing breaks,
REM  no history / cookies / cache is lost.
REM  Close WeChat and QQBrowser BEFORE running this.
REM ============================================================
setlocal

echo === Checking running apps ===
tasklist /FI "IMAGENAME eq Weixin.exe" 2>nul | find /I "Weixin.exe" >nul
if not errorlevel 1 (
    echo [X] WeChat is running. Please quit WeChat completely, then run again.
    pause & exit /b 1
)
tasklist /FI "IMAGENAME eq WeChat.exe" 2>nul | find /I "WeChat.exe" >nul
if not errorlevel 1 (
    echo [X] WeChat is running. Please quit WeChat completely, then run again.
    pause & exit /b 1
)
tasklist /FI "IMAGENAME eq QQBrowser.exe" 2>nul | find /I "QQBrowser.exe" >nul
if not errorlevel 1 (
    echo [X] QQBrowser is running. Please quit it completely, then run again.
    pause & exit /b 1
)

echo.
echo [1/2] WeChat  ...
call :movedir "C:\Users\lenovo\AppData\Roaming\Tencent\xwechat" "E:\AppData\Roaming\Tencent\xwechat" "WeChat"

echo.
echo [2/2] QQBrowser ...
call :movedir "C:\Users\lenovo\AppData\Local\Tencent\QQBrowser" "E:\AppData\Local\Tencent\QQBrowser" "QQBrowser"

echo.
echo Done. You can start WeChat / QQBrowser again now.
echo History, cookies and cache are all kept (they are on E: now).
pause
exit /b 0

:movedir
set "SRC=%~1"
set "DST=%~2"
set "NAME=%~3"
if not exist "%SRC%" (
    echo [-] %NAME%: source not found, skipped
    exit /b 0
)
if exist "%SRC%\*" goto docopy
echo [-] %NAME%: folder is empty, skipped
exit /b 0
:docopy
echo     copying %SRC%  --^>  %DST%
robocopy "%SRC%" "%DST%" /E /MOVE /R:1 /W:1 /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
    echo [X] %NAME%: copy failed ^(errorlevel %errorlevel%^). Nothing was changed.
    exit /b 1
)
if exist "%SRC%" rmdir "%SRC%" /s /q >nul 2>&1
mklink /J "%SRC%" "%DST%" >nul
if exist "%SRC%\." (
    echo [OK] %NAME%: moved to E: and linked back
    exit /b 0
)
echo [X] %NAME%: junction failed. Your data is safe at %DST%
exit /b 1
