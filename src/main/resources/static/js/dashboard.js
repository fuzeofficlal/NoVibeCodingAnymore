import { api, openMarketSocket } from "./api.js";
import { bindModal, buildUniverse, drawLineChart, formatMoney, formatPercent, formatQuantity, fullDate, getPortfolioContext, renderAllocation, renderAllocationBreakdown, renderMarketCards, setHTML, setText, toast } from "./ui.js";

const state = {
  previousPrices: {},
  metadata: new Map()
};

function renderTopline(summary, portfolio) {
  setText("portfolioIdPill", portfolio.portfolioId || "--");
  setText("portfolioName", portfolio.name || "Portfolio");
  setText("cashBalance", formatMoney(portfolio.cashBalance));
  setText("totalValue", formatMoney(summary.totalPortfolioValue));
  const roiNode = document.getElementById("totalReturn");
  const roi = Number(summary.totalReturnPercentage || 0);
  roiNode.textContent = formatPercent(roi);
  roiNode.className = `kpi-value ${roi >= 0 ? "positive" : "negative"}`;
  renderAllocation(document.getElementById("allocation"), summary.allocation, Number(summary.totalPortfolioValue || 0));
  renderAllocationBreakdown(document.getElementById("allocationStats"), summary.allocation, Number(summary.totalPortfolioValue || 0));
}

function renderHoldings(holdings) {
  const table = document.getElementById("holdingsTable");
  if (!holdings.length) {
    table.innerHTML = `<tr><td colspan="6" class="table-empty">No holdings yet. Use the trade page to buy stocks, bonds, or crypto.</td></tr>`;
    return;
  }
  table.innerHTML = holdings.map((item) => {
    const pl = Number(item.pl || 0);
    return `
      <tr>
        <td><strong>${item.symbol}</strong><div class="muted">${item.companyName || "Market asset"}</div></td>
        <td>${formatQuantity(item.shares)}</td>
        <td>${formatMoney(item.currentPrice)}</td>
        <td>${formatMoney(item.costBasis)}</td>
        <td>${formatMoney(item.marketValue)}</td>
        <td class="${pl >= 0 ? "positive" : "negative"}">${formatMoney(pl)}</td>
      </tr>
    `;
  }).join("");
}

function renderTransactions(transactions) {
  const body = document.getElementById("transactionsTable");
  if (!transactions.length) {
    body.innerHTML = `<tr><td colspan="5" class="table-empty">No transactions recorded yet.</td></tr>`;
    return;
  }
  body.innerHTML = transactions.map((item) => `
    <tr>
      <td>${item.transactionType}</td>
      <td>${item.tickerSymbol || "--"}</td>
      <td>${formatQuantity(item.quantity)}</td>
      <td>${formatMoney(item.pricePerUnit)}</td>
      <td>${fullDate(item.transactionDate)}</td>
    </tr>
  `).join("");
}

function renderPerformance(performance) {
  const canvas = document.getElementById("performanceChart");
  drawLineChart(canvas, [{
    label: "Portfolio NAV",
    color: "#f2a23a",
    data: performance.map((point) => ({ x: point.date, y: Number(point.value || 0) }))
  }]);
}

function buildMarketPayload(prices, universe) {
  const metadata = state.metadata;
  return Object.entries(universe).flatMap(([type, tickers]) =>
    tickers.slice(0, 6).map((ticker) => ({
      ticker,
      type,
      currentPrice: Number(prices[ticker]?.currentPrice || 0),
      previousPrice: Number(prices[ticker]?.previousPrice || 0),
      sparkline: prices[ticker]?.sparkline || [],
      name: metadata.get(ticker)?.name || metadata.get(ticker)?.companyName || ticker
    }))
  );
}

async function buildInitialSnapshots(tickers) {
  const histories = await api.getAssetHistoryBatch(tickers);
  return Object.fromEntries(Object.entries(histories).map(([ticker, series]) => {
    const sample = series.slice(-14);
    const latest = sample[sample.length - 1];
    const previous = sample[sample.length - 2] || latest;
    return [ticker, {
      currentPrice: Number(latest?.closePrice || latest?.close_price || 0),
      previousPrice: Number(previous?.closePrice || previous?.close_price || latest?.closePrice || 0),
      sparkline: sample.map((item) => Number(item.closePrice || item.close_price || 0))
    }];
  }));
}

function splitMarketCards(payload) {
  const compact = [
    ...payload.filter((item) => item.type === "STOCK").slice(0, 2),
    ...payload.filter((item) => item.type === "BOND").slice(0, 2),
    ...payload.filter((item) => item.type === "CRYPTO").slice(0, 2)
  ];
  renderMarketCards(document.getElementById("marketCompactBoard"), compact, state.previousPrices);
}

async function loadDashboard() {
  bindModal();
  const portfolio = getPortfolioContext();

  try {
    const assets = await api.getAssets();
    assets.forEach((asset) => state.metadata.set(asset.symbol, asset));

    const universe = buildUniverse(assets);
    const featuredTickers = [...universe.STOCK.slice(0, 2), ...universe.BOND.slice(0, 2), ...universe.CRYPTO.slice(0, 2)];
    const priceMap = await buildInitialSnapshots(featuredTickers);
    const payload = buildMarketPayload(priceMap, universe);
    splitMarketCards(payload);
    Object.assign(state.previousPrices, Object.fromEntries(Object.entries(priceMap).map(([ticker, value]) => [ticker, value.currentPrice])));

    if (!portfolio.id) {
      setHTML("dashboardError", `<div class="status-banner">Dashboard is open. Create or resume a portfolio from the home page to load account balances, holdings, and transaction history.</div>`);
      setText("portfolioName", "Market Dashboard");
      setText("portfolioIdPill", "--");
      setText("cashBalance", formatMoney(0));
      setText("totalValue", formatMoney(0));
      setText("totalReturn", formatPercent(0));
      renderHoldings([]);
      renderTransactions([]);
      renderPerformance([]);
      renderAllocation(document.getElementById("allocation"), {}, 0);
      renderAllocationBreakdown(document.getElementById("allocationStats"), {}, 0);
      openMarketSocket((packet) => {
        const next = Object.fromEntries(Object.entries(packet.prices || {}).map(([ticker, value]) => [ticker, {
          currentPrice: Number(value),
          previousPrice: Number(state.previousPrices[ticker] || value),
          sparkline: []
        }]));
        splitMarketCards(buildMarketPayload({ ...priceMap, ...next }, universe));
        Object.assign(state.previousPrices, Object.fromEntries(Object.entries(next).map(([ticker, value]) => [ticker, value.currentPrice])));
      }, () => {});
      return;
    }

    const [info, summary, holdings, performance, transactions] = await Promise.all([
      api.getPortfolio(portfolio.id),
      api.getSummary(portfolio.id),
      api.getHoldings(portfolio.id),
      api.getPerformance(portfolio.id, "1M"),
      api.getTransactions(portfolio.id)
    ]);

    renderTopline(summary, info);
    renderHoldings(holdings);
    renderTransactions(transactions);
    renderPerformance(performance);

    openMarketSocket((packet) => {
      const prices = packet.prices || {};
      const nextPayload = buildMarketPayload(Object.fromEntries(Object.entries(prices).map(([ticker, value]) => [ticker, {
        currentPrice: Number(value),
        previousPrice: Number(state.previousPrices[ticker] || value),
        sparkline: priceMap[ticker]?.sparkline || []
      }])), universe);
      splitMarketCards(nextPayload);
      Object.assign(state.previousPrices, prices);
      setText("marketTimestamp", `Streaming ${new Date(packet.timestamp || Date.now()).toLocaleTimeString("en-US")}`);
      setText("marketCompactMeta", "Compact benchmark strip");
    }, () => {
      toast("Market stream disconnected. Snapshot prices still available.", "error");
    });
  } catch (error) {
    setHTML("dashboardError", `<div class="status-banner">Unable to load dashboard: ${error.message}</div>`);
  }
}

async function forceMarketSync() {
  const btn = document.getElementById("forceSyncBtn");
  if (!btn) return;
  const originalText = btn.textContent;
  btn.textContent = "Syncing...";
  btn.disabled = true;
  try {
    await api.syncMarket();
    toast("Market data sync triggered successfully.", "success");
  } catch (error) {
    toast(`Sync Failed: ${error.message}`, "error");
  } finally {
    btn.textContent = originalText;
    btn.disabled = false;
  }
}

document.getElementById("forceSyncBtn")?.addEventListener("click", forceMarketSync);

loadDashboard();
