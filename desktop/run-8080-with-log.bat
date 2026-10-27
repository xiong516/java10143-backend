@echo off
rem ============================================================
rem  Java10143 backend - run detached with log
rem  ASCII ONLY. Port 8080 (JD / 京东). Coffee is 8082 - do not touch.
rem ============================================================
cd /d "%~dp0.."

set "JAVA_EXE=C:\Users\lenovo\.jdks\corretto-1.8.0_502\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

set "JAR=%~dp0..\target\Java10143.jar"

"%JAVA_EXE%" -Xms128m -Xmx512m -jar "%JAR%" --server.port=8080 > "%~dp0..\run-out.log" 2>&1
