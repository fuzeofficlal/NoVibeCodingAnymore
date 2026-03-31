@echo off
title Microservices Startup Script

echo ============================================================
echo   NoVibeCodingAnymore - Microservices Launcher
echo   [ Sequence: Cleanup - Go - Java - Python ]
echo ============================================================
echo.

echo [0/4] Sweeping old processes on Ports 8090, 8080, 8081, 8000...
for %%P in (8090 8080 8081 8000) do (
    for /f "tokens=5" %%T in ('C:\Windows\System32\netstat.exe -ano ^| C:\Windows\System32\findstr.exe :%%P') do (
        if not "%%T"=="0" (
            echo   - Killing PID %%T occupying Port %%P
            C:\Windows\System32\taskkill.exe /f /pid %%T >nul 2>&1
        )
    )
)
echo   Done cleaning up dangling ports!
echo.
C:\Windows\System32\timeout.exe /t 1 /nobreak >nul

echo [1/4] Starting Go API Gateway (:8090)...
cd gateway
start "Go API Gateway (Port 8090)" C:\Windows\System32\cmd.exe /k "go run main.go"
cd ..
C:\Windows\System32\timeout.exe /t 3 /nobreak >nul

echo.
echo [2/4] Starting Java Spring Boot Backend (:8080)...
REM Auto-inject JAVA_HOME specific to this user's IntelliJ IDEA setup
start "Java Spring Boot (Port 8080)" C:\Windows\System32\cmd.exe /k "set JAVA_HOME=%USERPROFILE%\.jdks\openjdk-25.0.1&& .\mvnw.cmd spring-boot:run"
C:\Windows\System32\timeout.exe /t 6 /nobreak >nul

echo.
echo [3/4] Starting AI Advisor Microservice (:8081)...
cd advisor-service
start "Spring AI Advisor (Port 8081)" C:\Windows\System32\cmd.exe /k "set JAVA_HOME=%USERPROFILE%\.jdks\openjdk-25.0.1&& ..\mvnw.cmd spring-boot:run"
cd ..
C:\Windows\System32\timeout.exe /t 6 /nobreak >nul

echo.
echo [4/4] Starting Python Market Data Service (:8000)...
cd market-service
start "Python FastAPI (Port 8000)" C:\Windows\System32\cmd.exe /k ".\venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000"
cd ..

echo.
echo ============================================================
echo   OK! All 4 microservices have been launched.
echo   Please check the four new pop-up terminal windows:
echo     1. Go Gateway : localhost:8090
echo     2. Java Core  : localhost:8080
echo     3. Spring AI  : localhost:8081
echo     4. Python ETL : localhost:8000
echo ============================================================
pause
