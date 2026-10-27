@echo off
rem ============================================================
rem  Locate a JDK (this project is JDK 8 based, JDK 8 accepted)
rem  NOTE: keep this file pure ASCII - a GBK console breaks on
rem        UTF-8 comments and eats line breaks.
rem ============================================================
set "JDK_HOME="

call :try "E:\xiazai\DevEco Studio\jbr"
call :try "%USERPROFILE%\.jdks\corretto-1.8.0_502"
call :try "C:\Users\lenovo\.jdks\corretto-1.8.0_502"
call :try "%JAVA_HOME%"
call :try "C:\Program Files\Huawei\DevEco Studio\jbr"
call :try "D:\DevEco Studio\jbr"
call :try "%USERPROFILE%\.jdks\azul-26.0.2"
call :try "C:\Program Files\Java\jdk-17"
call :try "C:\Program Files\Java\jdk-21"
call :try "C:\Program Files\Java\jdk1.8.0_202"

rem last resort: java on PATH
if not defined JDK_HOME (
  for /f "delims=" %%p in ('where java 2^>nul') do (
    call :try "%%~dpp"
  )
)
goto :eof

:try
if "%~1"=="" goto :eof
if defined JDK_HOME goto :eof
if not exist "%~1\bin\java.exe" goto :eof

set "VER="
set "VTMP=%TEMP%\java10143_ver.txt"
"%~1\bin\java.exe" -version > "%VTMP%" 2>&1
if not exist "%VTMP%" goto :eof
for /f "tokens=3" %%v in ('findstr /i "version" "%VTMP%"') do set "VER=%%~v"
del "%VTMP%" >nul 2>nul
if not defined VER goto :eof

rem accept 1.8 and everything 9+; skip 1.7 and older
echo %VER% | findstr /b "1.8" >nul && set "JDK_HOME=%~1" && goto :eof
echo %VER% | findstr /b "1." >nul && goto :eof
set "JDK_HOME=%~1"
goto :eof
