@echo off
C:\Windows\System32\chcp.com 65001 >nul
setlocal EnableDelayedExpansion

echo.
echo ============================================================
echo   Super Great OutStanding Shocking Impacting Cool Tests
echo ============================================================
echo.

set "rootDir=%~dp0"
if "%rootDir:~-1%"=="\" set "rootDir=%rootDir:~0,-1%"
cd /d "%rootDir%"

echo [INFO] Bypassing Execution Policy and launching PowerShell E2E Suite...
echo.

powershell -ExecutionPolicy Bypass -NoProfile -File .\full_e2e_test.ps1

echo.
echo ============================================================
echo   [DONE] E2E Test Suite Execution Complete.
echo ============================================================
pause
