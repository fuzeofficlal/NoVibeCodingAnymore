# 设定遇到错误时继续执行
$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "==============================================" -ForegroundColor DarkYellow
Write-Host "   🚀 启动全栈引擎并执行 E2E 核爆测试 🚀   " -ForegroundColor DarkYellow
Write-Host "==============================================`n" -ForegroundColor DarkYellow

$rootDir = "c:\Users\fuzeofficial\IdeaProjects\NoVibeCodingAnymore"

Write-Host "==============================================" -ForegroundColor DarkYellow
Write-Host "   ⚙️ AI 大模型全局鉴权配置" -ForegroundColor DarkYellow
Write-Host "==============================================`n" -ForegroundColor DarkYellow
$globalApiKey = Read-Host "🔑 请手动输入您的 OpenAI/Gemini API Key (直接回车跳过，可在前端手动化传入)"
if (-not [string]::IsNullOrWhiteSpace($globalApiKey)) {
    $env:OPENAI_API_KEY = $globalApiKey.Trim()
    Write-Host "✅ 全局密钥已就绪！微服务将在底层自动继承接管。`n" -ForegroundColor Green
} else {
    Write-Host "⚠️ 未输入全局密钥，微服务将工作在纯净模式，请确保在接口请求头提供 X-API-Key。`n" -ForegroundColor Yellow
}

# 1. 启动 Java Spring Boot (Core)
Write-Host ">>> [1/6] 正在拉起 Java Spring Boot 主防线 (独立窗口)..." -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" -ArgumentList "/c title [Spring Boot Core] && .\mvnw.cmd spring-boot:run" -WorkingDirectory $rootDir -WindowStyle Normal

# 2. 启动 Java Spring Boot (Advisor)
Write-Host ">>> [2/6] 正在拉起 Java 智能投顾微服务 (独立窗口)..." -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" -ArgumentList "/c title [Spring Boot Advisor] && ..\mvnw.cmd spring-boot:run -f pom.xml" -WorkingDirectory "$rootDir\advisor-service" -WindowStyle Normal

# 3. 启动 Python FastAPI (自动挂载虚拟环境)
Write-Host ">>> [3/6] 正在拉起 FastAPI 市场数据微服务 (独立窗口)..." -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" -ArgumentList "/c title [FastAPI Market] && .\venv\Scripts\uvicorn.exe main:app --reload" -WorkingDirectory "$rootDir\market-service" -WindowStyle Normal

# 4. 启动 Go Gateway (网关)
Write-Host ">>> [4/6] 正在拉起 Go 统一网关守护程序 (独立窗口)..." -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" -ArgumentList "/c title [Go Gateway] && go run main.go" -WorkingDirectory "$rootDir\gateway" -WindowStyle Normal

# 5. 预热缓冲时间
Write-Host "`n⏳ 正在等待四大引擎预热并向 8080/8081/8000/8090 开放监听（大约需要等30秒）..." -ForegroundColor Yellow

for ($i=1; $i -le 30; $i++) {
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 1
}
Write-Host " 预热完毕！" -ForegroundColor Green

# 6. 执行自动化脚本
Write-Host "`n>>> [6/6] 一切就绪！发射最终自动化 E2E 验收导弹！" -ForegroundColor Magenta
Write-Host "----------------------------------------------`n" -ForegroundColor Magenta

Set-Location $rootDir
powershell -ExecutionPolicy Bypass -File .\full_e2e_test.ps1
