import { api } from "./api.js";
import { bindModal, buildUniverse, getPortfolioContext, setHTML, setText, showModal, toast } from "./ui.js";

const state = {
  assets: [],
  universe: { STOCK: [], BOND: [], CRYPTO: [], ASHARE: [] },
  watchlist: [],
  alerts: [],
  newsData: null
};

function populateSelect(type) {
  const datalist = document.getElementById("watchlistTickerList");
  const input = document.getElementById("watchlistTicker");
  const tickers = state.universe[type] || [];
  datalist.innerHTML = tickers.map((ticker) => `<option value="${ticker}">${ticker}</option>`).join("");
  if (tickers.length > 0 && !tickers.includes(input.value)) input.value = tickers[0];
}

function renderWatchlist() {
  const mount = document.getElementById("watchlistSymbols");
  if (!state.watchlist.length) {
    mount.innerHTML = `<div class="empty-state">No watchlist symbols yet. Add a stock, bond, or crypto to start tracking it.</div>`;
    return;
  }
  mount.innerHTML = state.watchlist.map((item) => `
    <div class="watch-chip">
      <div>
        <div class="ticker">${item.tickerSymbol}</div>
        <div class="muted">Portfolio watchlist entry</div>
      </div>
      <div class="chip-actions">
        <button class="chip-action" type="button" data-remove="${item.tickerSymbol}">Remove</button>
      </div>
    </div>
  `).join("");

  mount.querySelectorAll("[data-remove]").forEach((button) => {
    button.addEventListener("click", async () => {
      const context = getPortfolioContext();
      try {
        await api.removeWatchlist(context.id, button.dataset.remove);
        toast(`${button.dataset.remove} removed from watchlist.`);
        await loadWatchlist();
      } catch (error) {
        showModal("Remove failed", error.message, "error");
      }
    });
  });
}

async function loadAlertFocus() {
  const context = getPortfolioContext();
  if (!context.id) return;
  try {
    const holdings = await api.getHoldings(context.id).catch(() => []);
    const watchlist = state.watchlist || [];
    const symbolSet = new Set();
    holdings.forEach(h => {
      const sym = h.symbol || h.tickerSymbol;
      if (sym) symbolSet.add(sym);
    });
    watchlist.forEach(w => {
      const sym = w.tickerSymbol || w.symbol;
      if (sym) symbolSet.add(sym);
    });
    
    const sorted = Array.from(symbolSet).sort();
    const datalist = document.getElementById("alertTickerList");
    const input = document.getElementById("alertTicker");
    if (!datalist) return;
    datalist.innerHTML = sorted.map(ticker => `<option value="${ticker}">${ticker}</option>`).join("");
    if (sorted.length > 0 && !sorted.includes(input.value)) input.value = sorted[0];
  } catch (err) {
    console.warn("Failed to resolve alert focus symbols", err);
  }
}

function renderAlerts() {
  const mount = document.getElementById("alertList");
  if (!mount) return;
  if (!state.alerts.length) {
    mount.innerHTML = `<div class="empty-state">No price alerts active.</div>`;
    return;
  }
  mount.innerHTML = state.alerts.map((item) => `
    <div class="watch-chip">
      <div>
        <div class="ticker">${item.tickerSymbol}</div>
        <div class="muted">${item.alertType === "TAKE_PROFIT" ? "Take Profit >=" : "Stop Loss <="} $${Number(item.targetPrice).toFixed(2)}</div>
      </div>
      <div class="chip-actions">
        <button class="chip-action" type="button" data-remove-alert="${item.id}">Delete</button>
      </div>
    </div>
  `).join("");

  mount.querySelectorAll("[data-remove-alert]").forEach((button) => {
    button.addEventListener("click", async () => {
      const context = getPortfolioContext();
      try {
        await api.removeAlert(context.id, button.dataset.removeAlert);
        toast(`Alert removed.`);
        await loadAlerts();
      } catch (error) {
        showModal("Remove failed", error.message, "error");
      }
    });
  });
}

async function loadAlerts() {
  const context = getPortfolioContext();
  if (!context.id) return;
  try {
    state.alerts = await api.getAlerts(context.id);
    renderAlerts();
  } catch (err) {
    console.warn("Failed to load alerts", err);
  }
}

async function renderNews() {
  const mount = document.getElementById("watchlistNews");
  const filter = document.getElementById("newsFilter");
  
  if (!state.watchlist.length) {
    mount.innerHTML = `<div class="empty-state">Add symbols to the watchlist to load related news.</div>`;
    if (filter) filter.style.display = "none";
    return;
  }

  try {
    const news = await api.getMarketNews(state.watchlist.map((item) => item.tickerSymbol));
    state.newsData = news;
    
    if (filter) {
      filter.innerHTML = `<option value="ALL">All Symbols</option>` + 
        Object.keys(news).map(ticker => `<option value="${ticker}">${ticker}</option>`).join("");
      filter.style.display = "block";
      filter.value = "ALL";
    }

    displayNews("ALL");
  } catch (error) {
    mount.innerHTML = `<div class="status-banner">Yahoo Finance news is wired in, but the local market gateway on port 8090 is currently not responding, so news fetch cannot complete right now.</div>`;
  }
}

function displayNews(selectedTicker) {
  const mount = document.getElementById("watchlistNews");
  if (!state.newsData) return;
  
  let cards = [];
  if (selectedTicker === "ALL") {
    cards = Object.entries(state.newsData).flatMap(([ticker, stories]) =>
      (stories || []).map((story) => `
        <article class="news-card">
          <div class="news-meta">${ticker} • ${story.publisher || "Yahoo Finance"}</div>
          <h4><a href="${story.link}" target="_blank" rel="noreferrer">${story.title || "Untitled article"}</a></h4>
        </article>
      `)
    );
  } else {
    cards = (state.newsData[selectedTicker] || []).map((story) => `
      <article class="news-card">
        <div class="news-meta">${selectedTicker} • ${story.publisher || "Yahoo Finance"}</div>
        <h4><a href="${story.link}" target="_blank" rel="noreferrer">${story.title || "Untitled article"}</a></h4>
      </article>
    `);
  }
  
  mount.innerHTML = cards.join("") || `<div class="empty-state">No news articles were returned for the current selection.</div>`;
}

async function loadWatchlist() {
  const context = getPortfolioContext();
  if (!context.id) {
    setHTML("watchlistStatus", `<div class="status-banner">Open or resume a portfolio first. The watchlist is bound to a portfolio ID.</div>`);
    renderWatchlist();
    renderNews();
    return;
  }
  setText("watchlistPortfolioId", context.id);
  state.watchlist = await api.getWatchlist(context.id);
  renderWatchlist();
  await renderNews();
  await loadAlertFocus();
}

async function init() {
  bindModal();
  try {
    state.assets = await api.getAssetsDirect();
    state.universe = buildUniverse(state.assets);
    populateSelect("STOCK");
    await loadWatchlist();
    await loadAlerts();
  } catch (error) {
    setHTML("watchlistStatus", `<div class="status-banner">Unable to initialize watchlist page: ${error.message}</div>`);
  }
}

document.getElementById("watchlistType")?.addEventListener("change", (event) => populateSelect(event.target.value));
document.getElementById("newsFilter")?.addEventListener("change", (event) => displayNews(event.target.value));
document.getElementById("watchlistForm")?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const context = getPortfolioContext();
  const ticker = document.getElementById("watchlistTicker").value;
  if (!context.id) {
    showModal("Portfolio required", "Create or resume a portfolio before adding watchlist symbols.", "error");
    return;
  }
  try {
    await api.addWatchlist(context.id, { tickerSymbol: ticker });
    toast(`${ticker} added to watchlist.`);
    await loadWatchlist();
  } catch (error) {
    showModal("Add failed", error.message, "error");
  }
});

document.getElementById("alertForm")?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const context = getPortfolioContext();
  if (!context.id) {
    showModal("Portfolio required", "Create or resume a portfolio before setting alerts.", "error");
    return;
  }
  const ticker = document.getElementById("alertTicker").value;
  const type = document.getElementById("alertType").value;
  const target = parseFloat(document.getElementById("alertTarget").value);
  if (isNaN(target) || target <= 0) return;
  
  try {
    await api.addAlert(context.id, { tickerSymbol: ticker, alertType: type, targetPrice: target });
    toast(`Alert set for ${ticker}.`);
    document.getElementById("alertForm").reset();
    await loadAlerts();
  } catch (error) {
    showModal("Alert failed", error.message, "error");
  }
});

init();
