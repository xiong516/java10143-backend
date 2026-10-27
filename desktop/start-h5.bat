@echo off
setlocal
rem ============================================================
rem  Java10143  H5 dev server (phone accessible)
rem  ASCII ONLY - do not put Chinese characters in this file
rem
rem  Why this script exists:
rem  1) HBuilderX GUI "run to browser" binds localhost only,
rem     so the phone cannot open it.
rem     -> we pass --host 0.0.0.0 explicitly.
rem  2) Do NOT create a vite.config.js in the uni-app project:
rem     it would replace HBuilderX own config file (plugins: [uni()])
rem     and break the vue plugin chain ("Failed to resolve ./App").
rem ============================================================

set "HBX=E:\Users\lenovo\Downloads\HBuilderX.5.26.2026091802\HBuilderX"
set "PROJ=E:\xiazai\huawei\Java10143\uni-app"
set "CLI=%HBX%\plugins\uniapp-cli-vite"
set "NODE=%HBX%\plugins\node\node.exe"

if not exist "%NODE%" (
  echo [ERROR] HBuilderX node.exe not found:
  echo         %NODE%
  echo         Edit this file and fix the HBX path.
  pause
  exit /b 1
)
if not exist "%CLI%\node_modules\@dcloudio\vite-plugin-uni\bin\uni.js" (
  echo [ERROR] uni-app compiler plugin not found under:
  echo         %CLI%
  pause
  exit /b 1
)

rem ---- stop any old instance holding port 5173 ----
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
  echo   stopping old process %%a on port 5173 ...
  taskkill /PID %%a /F >nul 2>&1
)

set "UNI_INPUT_DIR=%PROJ%"
set "UNI_OUTPUT_DIR=%PROJ%\unpackage\dist\dev\.uniapp.h5"
set "UNI_PLATFORM=h5"
set "NODE_ENV=development"

echo.
echo ============================================================
echo   H5 dev server starting...
echo.
echo   PC    : http://localhost:5173
echo   Phone : http://192.168.100.200:5173   (WiFi)
echo   Phone : http://192.168.103.21:5173    (Ethernet)
echo.
echo   Backend must be running on 8080 first
echo   (desktop\start-java10143.bat).
echo   Press Ctrl+C to stop.
echo ============================================================
echo.

cd /d "%CLI%"
"%NODE%" node_modules\@dcloudio\vite-plugin-uni\bin\uni.js --host 0.0.0.0 --port 5173
pause
