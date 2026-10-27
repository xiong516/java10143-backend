@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title Java10143 - BUILD
cd /d "%~dp0.."

echo ============================================================
echo   Java10143  backend build (Maven)
echo ============================================================
echo   Project dir : %CD%
echo.

rem ---------- 1. locate a JDK 8+ ----------
set "JAVA_HOME_OK="
call "%~dp0find-java.bat" 2>nul
if defined JDK_HOME set "JAVA_HOME_OK=%JDK_HOME%"

if not defined JAVA_HOME_OK (
  echo [ERROR] JDK 8+ not found.
  echo         Install JDK 8+ ,
  echo         then run this script again.
  pause
  exit /b 1
)
echo [OK] JAVA_HOME = %JAVA_HOME_OK%
set "JAVA_HOME=%JAVA_HOME_OK%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ---------- 2. locate maven ----------
set "MVN_CMD="
where mvn.cmd >nul 2>nul && set "MVN_CMD=mvn.cmd"

if not defined MVN_CMD (
  for /d %%a in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-*") do (
    for /d %%b in ("%%a\*") do (
      if exist "%%b\apache-maven-*\bin\mvn.cmd" (
        for /d %%c in ("%%b\apache-maven-*") do set "MVN_CMD=%%c\bin\mvn.cmd"
      )
    )
  )
)

if not defined MVN_CMD (
  echo [ERROR] Maven not found. Install Maven or open the project in IDEA and
  echo         let the IDE download a Maven wrapper, then retry.
  pause
  exit /b 1
)
echo [OK] Maven      = %MVN_CMD%
echo.
echo Building, first run downloads dependencies, please wait...
echo.

call "%MVN_CMD%" -B -DskipTests clean package
if errorlevel 1 (
  echo.
  echo [ERROR] Build failed, see the log above.
  pause
  exit /b 1
)

echo.
echo [OK] Build finished:  %CD%\target\Java10143.jar
pause
