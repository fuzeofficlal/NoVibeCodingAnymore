# 设定遇到错误时继续执行
$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "==============================================" -ForegroundColor DarkYellow
Write-Host "   🚀 启动全栈引擎并执行 E2E 核爆测试 🚀   " -ForegroundColor DarkYellow
Write-Host "==============================================`n" -ForegroundColor DarkYellow

$rootDir = "c:\Users\fuzeofficial\IdeaProjects\NoVibeCodingAnymore"

# 1. 启动 Java Spring Boot
Write-Host ">>> [1/4] 正在拉起 Java Spring Boot 主防线 (独立窗口)..." -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" -ArgumentList "/c title [Spring Boot Backend] && .\mvnw.cmd spring-boot:run" -WorkingDirectory $rootDir -WindowStyle Normal

# 2. 启动 Python FastAPI (自动挂载虚拟环境)
Write-Host ">>> [2/4] 正在拉起 FastAPI 市场数据微服务 (独立窗口)..." -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" -ArgumentList "/c title [FastAPI MicroService] && .\venv\Scripts\uvicorn.exe main:app --reload" -WorkingDirectory "$rootDir\market-service" -WindowStyle Normal

# 3. 预热缓冲时间
Write-Host "`n⏳ 正在等待引擎预热并向 8080 / 8000 开放监听（大约需要等25秒）..." -ForegroundColor Yellow

for ($i=1; $i -le 25; $i++) {
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 1
}
Write-Host " 预热完毕！" -ForegroundColor Green

# 4. 执行自动化脚本
Write-Host "`n>>> [4/4] 一切就绪！发射最终自动化 E2E 验收导弹！" -ForegroundColor Magenta
Write-Host "----------------------------------------------`n" -ForegroundColor Magenta

Set-Location $rootDir
powershell -ExecutionPolicy Bypass -File .\e2e_test.ps1
