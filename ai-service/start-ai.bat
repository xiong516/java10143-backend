@echo off
chcp 65001 >nul
title Java10143 智能客服 (AI Service)

echo ============================================================
echo   Java10143 智能客服服务
echo ============================================================
echo.

rem === 找 Python：优先用系统 python，其次用 workbuddy 自带的 ===
set PY=
where python >nul 2>nul && set PY=python
if "%PY%"=="" (
  if exist "C:\Users\lenovo\.workbuddy\binaries\python\versions\3.13.12\python.exe" (
    set PY=C:\Users\lenovo\.workbuddy\binaries\python\versions\3.13.12\python.exe
  )
)
if "%PY%"=="" (
  echo [X] 没找到 Python，请先装一个 Python 3.9 以上版本： https://www.python.org/downloads/
  pause
  exit /b 1
)

echo [1/2] 使用 Python: %PY%
echo.

rem === 可选：接大模型。设了 AI_API_KEY 就走真模型，不设就用内置话术引擎 ===
if "%AI_API_KEY%"=="" (
  echo [提示] 当前没有设置 AI_API_KEY，使用「本地专家模式」，
  echo        内置多套话术 + 意图识别 + 随机变化，不需要联网也能聊。
  echo        想接真实大模型，先设置环境变量再启动，例如：
  echo          set AI_API_KEY=你的key
  echo          set AI_BASE_URL=https://api.deepseek.com/v1
  echo          set AI_MODEL=deepseek-chat
  echo.
)

echo [2/2] 启动服务 http://localhost:8010  （按 Ctrl+C 停止）
echo.
"%PY%" "%~dp0server.py" 8010

pause
