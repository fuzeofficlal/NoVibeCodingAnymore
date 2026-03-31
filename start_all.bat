@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo.
echo ==============================================
echo    [STARTING] System Engines and E2E Tests
echo ==============================================
echo.

set "rootDir=%~dp0"
if "%rootDir:~-1%"=="\" set "rootDir=%rootDir:~0,-1%"

echo ==============================================
echo    [CONFIG] AI Global API Key
echo ==============================================
echo.
set /p globalApiKey="Please input your OpenAI/Gemini API Key (Press Enter to skip): "

if not "!globalApiKey!"=="" (
    set "OPENAI_API_KEY=!globalApiKey!"
    echo [SUCCESS] Global API Key injected into environment variables!
    echo.
) else (
    echo [WARNING] No Global API Key provided. Make sure to use X-API-Key in HTTP headers.
    echo.
)

echo ^>^>^> [1/6] Starting Java Spring Boot Core (New Window)...
start "Spring Boot Core" /D "%rootDir%" cmd /c ".\mvnw.cmd spring-boot:run"

echo ^>^>^> [2/6] Starting Java Advisor Service (New Window)...
start "Spring Boot Advisor" /D "%rootDir%\advisor-service" cmd /c "..\mvnw.cmd spring-boot:run -f pom.xml"

echo ^>^>^> [3/6] Starting FastAPI Market Service (New Window)...
start "FastAPI Market" /D "%rootDir%\market-service" cmd /c ".\venv\Scripts\uvicorn.exe main:app --reload"

echo ^>^>^> [4/6] Starting Go Gateway (New Window)...
start "Go Gateway" /D "%rootDir%\gateway" cmd /c "go run main.go"

echo.
echo [WAIT] Waiting 30 seconds for all 4 engines to spin up and bind ports...
timeout /t 30 /nobreak >nul
echo [READY] Warmup complete!

echo.
echo ^>^>^> [6/6] Launching Full E2E Integration Tests...
echo ----------------------------------------------
echo.

cd /d "%rootDir%"
powershell -ExecutionPolicy Bypass -File .\full_e2e_test.ps1

echo.
pause
