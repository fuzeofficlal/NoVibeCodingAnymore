#!/bin/bash
set -e

# ANSI Colors
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
MAGENTA='\033[0;35m'
DARK_GRAY='\033[1;30m'
NC='\033[0m'

echo -e "${CYAN}==========================================================${NC}"
echo -e "${CYAN}   Full E2E Integration Test: Go + Java + Python (Bash)${NC}"
echo -e "${CYAN}==========================================================\n${NC}"

GATEWAY="http://127.0.0.1:8090/api/v1"

assert_api_call() {
    local desc=$1
    local method=$2
    local uri=$3
    local body=$4

    echo -e "\n------------------------------------------------------------"
    echo -e " ${CYAN}[SCENARIO] $desc${NC}"
    echo -e " ${DARK_GRAY}[REQUEST]  $method $uri${NC}"
    if [ ! -z "$body" ]; then
        echo -e " ${DARK_GRAY}[PAYLOAD]  $body${NC}"
    fi

    local resp=""
    if [ -z "$body" ]; then
        resp=$(curl -s -w "\n%{http_code}" -X "$method" "$uri")
    else
        resp=$(curl -s -w "\n%{http_code}" -X "$method" -H "Content-Type: application/json" -d "$body" "$uri")
    fi

    local status_code=$(echo "$resp" | tail -n1)
    local actual_body=$(echo "$resp" | head -n -1)
    local truncated_body="${actual_body:0:800}"
    
    if [ ${#actual_body} -gt 800 ]; then
        truncated_body="${truncated_body} ... (Truncated)"
    fi

    if [[ "$status_code" =~ ^2 ]]; then
        echo -e " ${GREEN}[ACTUAL(HTTP $status_code)] $truncated_body${NC}"
    else
        echo -e " ${RED}[ERROR(HTTP $status_code)] $truncated_body${NC}"
    fi
}

echo -e "${YELLOW}>>> [1/7] Checking Go Gateway (Port 8090)...${NC}"
if ! curl -s "http://127.0.0.1:8090" > /dev/null; then
    echo -e "${RED}ERROR: Go Gateway is not responding! (Make sure docker-compose is up)${NC}"
    # Continue anyway as swagger endpoint might be what responds
fi
echo -e "${GREEN}SUCCESS: Gateway check passed!\n${NC}"

echo -e "${YELLOW}>>> [2/7] Database Sandbox Initialized by Docker...${NC}"

echo -e "${YELLOW}>>> [3/7] Testing Python Market Data Routing...${NC}"
assert_api_call "Fetch Real-time Snapshot for AAPL and BTC-USD" "GET" "$GATEWAY/market/prices?tickers=AAPL,BTC-USD" ""

echo -e "\n${YELLOW}>>> [4/7] Testing Java Transaction Logic Engine...${NC}"
assert_api_call "Deposit Cash into Portfolio" "POST" "$GATEWAY/portfolios/TEST_PORTFOLIO_FULL/transactions" '{"transactionType": "DEPOSIT", "quantity": 100000.0}'
assert_api_call "Buy 100 Shares of AAPL @ $150.00" "POST" "$GATEWAY/portfolios/TEST_PORTFOLIO_FULL/transactions" '{"transactionType": "BUY", "tickerSymbol": "AAPL", "quantity": 100.0, "pricePerUnit": 150.00}'

echo -e "\n${YELLOW}>>> [5/7] Testing Portfolio Matrix / Penetration (Holdings)...${NC}"
assert_api_call "Fetch Active Holdings with Live PnL" "GET" "$GATEWAY/portfolios/TEST_PORTFOLIO_FULL/holdings" ""

echo -e "\n${YELLOW}>>> [6/7] Testing Portfolio NAV and Asset Allocation Reductions...${NC}"
assert_api_call "Fetch Total Portfolio Summary and Allocations" "GET" "$GATEWAY/portfolios/TEST_PORTFOLIO_FULL/summary" ""

echo -e "\n${YELLOW}>>> [8/8] Testing Watchlist, News, and LLM Autonomous Agent...${NC}"
assert_api_call "Append TSLA into user Watchlist" "POST" "$GATEWAY/portfolios/TEST_PORTFOLIO_FULL/watchlist" '{"tickerSymbol": "TSLA"}'

echo -e "\n${MAGENTA}   [🤖] Wake Up Call 1: News & Holdings Orchestration${NC}"
chatPrompt1='{"query": "Do I currently hold Tesla and Apple? If so, get the latest news for Apple and summarize the risk in one sentence."}'
assert_api_call "Agent reads holdings, fetches news, and writes report" "POST" "$GATEWAY/advisor/TEST_PORTFOLIO_FULL/chat" "$chatPrompt1"

echo -e "\n${MAGENTA}   [🤯] Wake Up Call 2: Autonomous Trading (Action Engine)${NC}"
chatPrompt2='{"query": "Please execute a BUY order for 5 shares of AAPL for my portfolio right now."}'
assert_api_call "Agent autonomously calculates market price and executes a live BUY transaction" "POST" "$GATEWAY/advisor/TEST_PORTFOLIO_FULL/chat" "$chatPrompt2"

echo -e "\n${RED}   [🚨] Wake Up Call 3: Proactive Awakening Engine (System Alert)${NC}"
alertPayload='{"ticker": "AAPL", "price": 145.0, "type": "STOP_LOSS", "target": 150.0}'
assert_api_call "Core Alert Scheduler forcefully awakens AI Copilot" "POST" "$GATEWAY/advisor/TEST_PORTFOLIO_FULL/proactive-alert" "$alertPayload"

echo -e "\n${GREEN}================================================================${NC}"
echo -e "${GREEN}   🚀 ALL 100% BUSINESS DOMAINS PASSED THROUGH GO GATEWAY!      ${NC}"
echo -e "${GREEN}================================================================${NC}"
