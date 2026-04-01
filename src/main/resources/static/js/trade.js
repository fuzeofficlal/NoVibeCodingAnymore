import { api, openMarketSocket } from "./api.js";
import { bindModal, buildUniverse, formatMoney, formatQuantity, getPortfolioContext, renderMarketCards, setHTML, setText, showModal, toast } from "./ui.js";

const state = {
  assetMap: new Map(),
  priceMap: {},
  holdingsMap: new Map(),
  cashBalance: 0,
  universe: { STOCK: [], BOND: [], CRYPTO: [] }
};

function populateTickerSelect(type) {
  const datalist = document.getElementById("tradeTickerList");
  const input = document.getElementById("tradeTicker");
  const tickers = state.universe[type] || [];
  datalist.innerHTML = tickers.map((ticker) => `<option value="${ticker}">${state.assetMap.get(ticker)?.name || ticker}</option>`).join("");
  if (tickers.length > 0 && !tickers.includes(input.value)) input.value = tickers[0];
  updateQuotePreview();
  syncSelectedQuote();
}

async function refreshPortfolio() {
  const context = getPortfolioContext();
  const [portfolio, holdings, transactions] = await Promise.all([
    api.getPortfolio(context.id),
    api.getHoldings(context.id),
    api.getTransactions(context.id)
  ]);

  state.cashBalance = Number(portfolio.cashBalance || 0);
  setText("tradePortfolioName", portfolio.name);
  setText("tradeBalance", formatMoney(portfolio.cashBalance));
  holdings.forEach((item) => state.holdingsMap.set(item.symbol, Number(item.shares || 0)));
  document.getElementById("recentTrades").innerHTML = transactions.slice(0, 8).map((item) => `
    <div class="list-item">
      <div class="row-between"><strong>${item.transactionType}</strong><span>${item.tickerSymbol || "Cash"}</span></div>
      <div class="muted">${formatQuantity(item.quantity)} @ ${formatMoney(item.pricePerUnit)}</div>
    </div>
  `).join("") || `<div class="empty-state">No recent transactions yet.</div>`;
}

async function loadTradePage() {
  bindModal();
  const context = getPortfolioContext();

  try {
    const assets = await api.getAssets();
    assets.forEach((asset) => state.assetMap.set(asset.symbol, asset));
    state.universe = buildUniverse(assets);
    populateTickerSelect("STOCK");

    const featured = [...state.universe.STOCK.slice(0, 3), ...state.universe.BOND.slice(0, 3), ...state.universe.CRYPTO.slice(0, 3)];
    const histories = await api.getAssetHistoryBatch(featured);
    state.priceMap = Object.fromEntries(Object.entries(histories).map(([ticker, series]) => {
      const latest = series[series.length - 1];
      return [ticker, Number(latest?.closePrice || latest?.close_price || 0)];
    }));
    const boardPayload = featured.map((ticker) => ({
      ticker,
      type: state.assetMap.get(ticker)?.type || (ticker.includes("-USD") ? "CRYPTO" : "STOCK"),
      currentPrice: state.priceMap[ticker] || 0,
      previousPrice: Number(histories[ticker]?.[histories[ticker].length - 2]?.closePrice || histories[ticker]?.[histories[ticker].length - 2]?.close_price || state.priceMap[ticker] || 0),
      sparkline: (histories[ticker] || []).slice(-12).map((item) => Number(item.closePrice || item.close_price || 0)),
      name: state.assetMap.get(ticker)?.name || ticker
    }));
    renderMarketCards(document.getElementById("tradeMarketBoard"), boardPayload);
    openMarketSocket((packet) => {
      const prices = packet.prices || {};
      const nextPayload = boardPayload.map((item) => ({
        ...item,
        previousPrice: item.currentPrice,
        currentPrice: Number(prices[item.ticker] || item.currentPrice)
      }));
      nextPayload.forEach((item) => {
        state.priceMap[item.ticker] = item.currentPrice;
      });
      renderMarketCards(document.getElementById("tradeMarketBoard"), nextPayload);
      updateQuotePreview();
    }, () => {});
    if (!context.id) {
      setHTML("tradeStatus", `<div class="status-banner">Trade page is open. Create or resume a portfolio before placing orders or moving cash.</div>`);
      setText("tradePortfolioName", "Execution Desk");
      setText("tradeBalance", formatMoney(0));
      document.getElementById("recentTrades").innerHTML = `<div class="empty-state">No active portfolio selected yet.</div>`;
      updateQuotePreview();
      return;
    }
    await refreshPortfolio();
    updateQuotePreview();
    syncSelectedQuote();
  } catch (error) {
    setHTML("tradeStatus", `<div class="status-banner">Unable to initialize trading page: ${error.message}</div>`);
  }
}

function updateQuotePreview() {
  const ticker = document.getElementById("tradeTicker").value;
  const quantity = Number(document.getElementById("tradeQuantity").value || 0);
  const price = Number(state.priceMap[ticker] || 0);
  const notional = price * quantity;
  setText("liveExecutionPrice", price ? formatMoney(price) : "--");
  setText("estimatedNotional", quantity ? formatMoney(notional) : "--");
  setText("availableCash", formatMoney(state.cashBalance));
  setText("currentHolding", formatQuantity(state.holdingsMap.get(ticker) || 0));
}

async function syncSelectedQuote() {
  const ticker = document.getElementById("tradeTicker").value;
  if (!ticker) return;
  try {
    const [quote] = await api.getPrices([ticker]);
    state.priceMap[ticker] = Number(quote?.current_price || 0);
    updateQuotePreview();
    
    try {
      setText("smaValue", "...");
      const days = document.getElementById("smaDays").value || 50;
      const smaData = await api.getSma(ticker, days);
      if (smaData && smaData.length > 0) {
        setText("smaValue", formatMoney(smaData[smaData.length - 1].sma));
      } else {
        setText("smaValue", "--");
      }
    } catch (e) {
      console.warn("SMA fetch error", e);
      setText("smaValue", "--");
    }

  } catch (error) {
    toast(`Unable to refresh price for ${ticker}: ${error.message}`, "error");
  }
}

async function submitTrade(event) {
  event.preventDefault();
  const context = getPortfolioContext();
  if (!context.id) {
    showModal("Portfolio required", "Create or resume a portfolio before submitting a trade.", "error");
    return;
  }
  const transactionType = document.getElementById("tradeSide").value;
  const ticker = document.getElementById("tradeTicker").value;
  const quantity = Number(document.getElementById("tradeQuantity").value);
  const price = Number(state.priceMap[ticker] || 0);

  if (!ticker || !(quantity > 0) || !(price > 0)) {
    showModal("Invalid order", "Choose an asset, enter a quantity greater than 0, and wait for a live price.", "error");
    return;
  }

  if (transactionType === "BUY" && state.cashBalance < quantity * price) {
    showModal("Insufficient balance", "Cash balance is not enough to pay for this order. Deposit more funds or reduce the order size.", "error");
    return;
  }

  if (transactionType === "SELL" && (state.holdingsMap.get(ticker) || 0) < quantity) {
    showModal("Insufficient position", "You are trying to sell more than the current holding size.", "error");
    return;
  }

  try {
    await api.processTransaction(context.id, {
      transactionType,
      tickerSymbol: ticker,
      quantity,
      pricePerUnit: price
    });
    toast(`${transactionType} ${ticker} executed.`);
    showModal("Order successful", `${transactionType} ${formatQuantity(quantity)} ${ticker} executed at ${formatMoney(price)}.`);
    document.getElementById("tradeQuantity").value = "";
    await refreshPortfolio();
    await syncSelectedQuote();
  } catch (error) {
    showModal("Order failed", error.message, "error");
  }
}

async function submitCashFlow(event) {
  event.preventDefault();
  const context = getPortfolioContext();
  if (!context.id) {
    showModal("Portfolio required", "Create or resume a portfolio before processing deposits or withdrawals.", "error");
    return;
  }
  const type = document.getElementById("cashAction").value;
  const amount = Number(document.getElementById("cashAmount").value);

  if (!(amount > 0)) {
    showModal("Invalid amount", "Cash flow amount must be greater than 0.", "error");
    return;
  }

  if (type === "WITHDRAW" && amount > state.cashBalance) {
    showModal("Insufficient balance", "Withdrawal amount exceeds available cash.", "error");
    return;
  }

  try {
    await api.processTransaction(context.id, {
      transactionType: type,
      quantity: amount,
      pricePerUnit: amount
    });
    toast(`${type} completed successfully.`);
    showModal("Cash flow successful", `${type === "DEPOSIT" ? "Deposit" : "Withdrawal"} of ${formatMoney(amount)} completed.`);
    document.getElementById("cashAmount").value = "";
    await refreshPortfolio();
  } catch (error) {
    showModal("Cash flow failed", error.message, "error");
  }
}

document.getElementById("assetType")?.addEventListener("change", (event) => populateTickerSelect(event.target.value));
document.getElementById("tradeTicker")?.addEventListener("change", syncSelectedQuote);
document.getElementById("tradeQuantity")?.addEventListener("input", updateQuotePreview);
document.getElementById("tradeSide")?.addEventListener("change", updateQuotePreview);
document.getElementById("smaDays")?.addEventListener("change", syncSelectedQuote);
document.getElementById("refreshQuote")?.addEventListener("click", syncSelectedQuote);
document.getElementById("tradeForm")?.addEventListener("submit", submitTrade);
document.getElementById("cashFlowForm")?.addEventListener("submit", submitCashFlow);

loadTradePage();
