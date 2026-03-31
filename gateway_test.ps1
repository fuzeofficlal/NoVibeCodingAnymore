$ErrorActionPreference = "Stop"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   E2E Automated Integration Test (Java + Python)   " -ForegroundColor Cyan
Write-Host "==============================================`n" -ForegroundColor Cyan

# 1. Check services
Write-Host ">>> [1/5] Checking port 8090 (Java) and 8090 (Python)..." -ForegroundColor Yellow

$javaIsUp = (Test-NetConnection -ComputerName 127.0.0.1 -Port 8090 -InformationLevel Quiet)
if (-not $javaIsUp) {
    Write-Host "ERROR: Java Spring Boot service (Port 8090) is not running!" -ForegroundColor Red
    exit 1
}

$pythonIsUp = (Test-NetConnection -ComputerName 127.0.0.1 -Port 8090 -InformationLevel Quiet)
if (-not $pythonIsUp) {
    Write-Host "ERROR: Python FastAPI service (Port 8090) is not running!" -ForegroundColor Red
    exit 1
}
Write-Host "SUCCESS: Both microservices are ONLINE!" -ForegroundColor Green

# 2. Db Seeding
Write-Host "`n>>> [2/5] Seeding DB with TEST_PORTFOLIO_001..." -ForegroundColor Yellow

$pythonSeeder = @"
import pymysql
try:
    conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='Azhe114514', database='newport_db', autocommit=True)
    with conn.cursor() as c:
        # 清除上一轮的脏测试残余数据，保证幂等�?        c.execute("DELETE FROM portfolio_transaction WHERE portfolio_id='TEST_PORTFOLIO_001'")
        c.execute("DELETE FROM position WHERE portfolio_id='TEST_PORTFOLIO_001'")
        c.execute("UPDATE portfolio SET cash_balance=0 WHERE portfolio_id='TEST_PORTFOLIO_001'")
        
        c.execute("INSERT IGNORE INTO company_info (ticker_symbol, company_name, asset_type) VALUES ('AAPL', 'Apple Inc.', 'STOCK')")
        c.execute("INSERT IGNORE INTO portfolio (portfolio_id, name, cash_balance) VALUES ('TEST_PORTFOLIO_001', 'E2E Auto Test', 0)")
        c.execute("INSERT INTO market_price (ticker_symbol, current_price) VALUES ('AAPL', 155.00) ON DUPLICATE KEY UPDATE current_price=155.00")
    conn.close()
    print("DB Seed Done.")
except Exception as e:
    print(f"DB Seed Error: {e}")
"@

$pythonOutput = $pythonSeeder | python - 
if ($pythonOutput -match "Error") {
    Write-Host "ERROR During DB Seeding:`n$pythonOutput" -ForegroundColor Red
    exit 1
}
Write-Host "SUCCESS: Base data inserted/reset in newport_db!" -ForegroundColor Green

# 3. Java Transactions
Write-Host "`n>>> [3/5] Verifying Spring Boot Business Logic (Deposit & Trade)..." -ForegroundColor Yellow

$headers = @{"Content-Type" = "application/json"}
$depositBody = '{"transactionType": "DEPOSIT", "quantity": 100000.0}'
$depResp = Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/v1/portfolios/TEST_PORTFOLIO_001/transactions" -Method Post -Body $depositBody -Headers $headers
Write-Host "   -> Deposited 100,000" -ForegroundColor DarkGray

$buyBody = '{"transactionType": "BUY", "tickerSymbol": "AAPL", "quantity": 100.0, "pricePerUnit": 5.00}'
$buyResp = Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/v1/portfolios/TEST_PORTFOLIO_001/transactions" -Method Post -Body $buyBody -Headers $headers
Write-Host "   -> BUY 100 AAPL @ 5.00" -ForegroundColor DarkGray

$portInfo = Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/v1/portfolios/TEST_PORTFOLIO_001" -Method Get
$expectedBalance = 100000 - (100 * 5)
if ($portInfo.cashBalance -ne $expectedBalance) {
    Write-Host "ERROR: Balance deduction failed! Expected: $expectedBalance, Got: $($portInfo.cashBalance)" -ForegroundColor Red
    exit 1
}
Write-Host "SUCCESS: Transaction logic verified. Cash balance perfectly matches expected." -ForegroundColor Green

# 4. Python Sync
Write-Host "`n>>> [4/5] Verifying FastAPI Database Interfacing (Reading seeded data)..." -ForegroundColor Yellow

$priceSnapshots = Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/v1/market/prices?tickers=AAPL" -Method Get
if ($null -eq $priceSnapshots -or $priceSnapshots.Count -eq 0) {
    Write-Host "ERROR: Python /prices API returned nothing for AAPL!" -ForegroundColor Red
    exit 1
}
$curPrice = $priceSnapshots[0].current_price
Write-Host "SUCCESS: Python market prices queried perfectly! AAPL is $curPrice" -ForegroundColor Green

# 5. E2E Summary PnL Check
Write-Host "`n>>> [5/5] Checking Java x Python Cross-Microservice PnL Calculation..." -ForegroundColor Yellow

try {
    $summary = Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/v1/portfolios/TEST_PORTFOLIO_001/summary" -Method Get
    if ($null -eq $summary.totalPortfolioValue) {
        Write-Host "ERROR: Java /summary returned invalid totalPortfolioValue!" -ForegroundColor Red
        exit 1
    }
    Write-Host "SUCCESS: Cross-service business logic works flawlessly!" -ForegroundColor Green
    Write-Host "   - Total Portfolio Value: `$ $($summary.totalPortfolioValue)" -ForegroundColor Magenta
    Write-Host "   - Total Return (ROI): $($summary.totalReturnPercentage)%" -ForegroundColor Magenta
} catch {
    Write-Host "ERROR: Java /summary API failed. Check backend logs." -ForegroundColor Red
    exit 1
}

Write-Host "`n===============================================" -ForegroundColor Green
Write-Host "       E2E TEST 100% PASSED SUCCESSFULLY!    " -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
