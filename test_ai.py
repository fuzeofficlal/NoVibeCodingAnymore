import requests
import time
import sys

baseUrl = "http://localhost:8090/api/v1"

print("1. Waiting for Gateway and Services to start...")
for i in range(120):
    try:
        res = requests.get(f"http://localhost:8090/api/v1/assets")
        if res.status_code == 200:
            print("\n✅ Gateway is UP!")
            break
    except:
        pass
    print(".", end="", flush=True)
    time.sleep(2)
else:
    print("\n❌ Gateway did not start in time. Exiting.")
    sys.exit(1)

# Check advisor specifically
print("2. Checking if Advisor Service (8081) is ready...")
for i in range(60):
    try:
        # even if it 404s or 400s, as long as it's not a Go 502 Bad Gateway
        res = requests.get(f"{baseUrl}/advisor/nonexistent/insight")
        if res.status_code != 502:
            print("\n✅ AI Advisor Service is UP!")
            break
    except:
        pass
    print(".", end="", flush=True)
    time.sleep(2)

print("\n3. Creating Mock Portfolio...")
create_res = requests.post(f"{baseUrl}/portfolios", json={
    "name": "Test Quant Portfolio",
    "initialDeposit": 1000000.00
})
if create_res.status_code != 201:
    print("❌ Failed to create portfolio:", create_res.text)
    sys.exit(1)

portfolio_id = create_res.json()["portfolioId"]
print(f"✅ Created Portfolio: {portfolio_id}")

print("\n4. Buying Assets for AI Analysis...")
assets_to_buy = [
    {"transactionType": "BUY", "tickerSymbol": "AAPL", "quantity": 100, "pricePerUnit": 150.0},
    {"transactionType": "BUY", "tickerSymbol": "BTC-USD", "quantity": 5, "pricePerUnit": 60000.0},
    {"transactionType": "BUY", "tickerSymbol": "600519.SS", "quantity": 100, "pricePerUnit": 1500.0}
]
for asset in assets_to_buy:
    res = requests.post(f"{baseUrl}/portfolios/{portfolio_id}/transactions", json=asset)
    print(f"👉 Bought {asset['quantity']} of {asset['tickerSymbol']} (Status: {res.status_code})")

print("\n5. Call Smart Advisor API (Waiting for AI Response...)")
start_time = time.time()
try:
    insight_res = requests.get(f"{baseUrl}/advisor/{portfolio_id}/insight", timeout=60)
    print(f"✅ AI Responded in {time.time() - start_time:.2f} seconds!")
    print("\n" + "="*80)
    print("🤖 THE REPORT:\n")
    print(insight_res.text)
    print("="*80)
except Exception as e:
    print("❌ Error querying AI:", e)
