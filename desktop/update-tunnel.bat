@echo off
chcp 65001 >nul
title Sync cpolar tunnel domain
setlocal
set "PY=E:\xiazai\python.exe"
if not exist "%PY%" set "PY=python"
echo ============================================================
echo   Sync the CURRENT cpolar tunnel domain into all frontends
echo     - WeChat mini program  (miniprogram-jd)
echo     - HarmonyOS app        (MyApplication10143)
echo     - uni-app              (Java10143/uni-app)
echo ============================================================
echo.
"%PY%" "E:\xiazai\huawei\Java10143\tools\sync_tunnel.py" %*
echo.
pause
