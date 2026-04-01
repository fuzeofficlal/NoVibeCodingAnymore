import { api } from "./api.js";
import { bindModal, buildUniverse, getPortfolioContext, setHTML, setText, showModal, toast } from "./ui.js";

const state = {
  assets: [],
  universe: { STOCK: [], BOND: [], CRYPTO: [] },
  watchlist: []
};

function populateSelect(type) {
  const select = document.getElementById("watchlistTicker");
  const options = (state.universe[type] || []).map((ticker) => `<option value="${ticker}">${ticker}</option>`).join("");
  select.innerHTML = options;
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

async function renderNews() {
  const mount = document.getElementById("watchlistNews");
  if (!state.watchlist.length) {
    mount.innerHTML = `<div class="empty-state">Add symbols to the watchlist to load related news.</div>`;
    return;
  }

  try {
    const news = await api.getMarketNews(state.watchlist.map((item) => item.tickerSymbol));
    const cards = Object.entries(news).flatMap(([ticker, stories]) =>
      (stories || []).map((story) => `
        <article class="news-card">
          <div class="news-meta">${ticker} • ${story.publisher || "Yahoo Finance"}</div>
          <h4><a href="${story.link}" target="_blank" rel="noreferrer">${story.title || "Untitled article"}</a></h4>
        </article>
      `)
    );
    mount.innerHTML = cards.join("") || `<div class="empty-state">No news articles were returned for the current watchlist.</div>`;
  } catch (error) {
    mount.innerHTML = `<div class="status-banner">Yahoo Finance news is wired in, but the local market gateway on port 8090 is currently not responding, so news fetch cannot complete right now.</div>`;
  }
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
}

async function init() {
  bindModal();
  try {
    state.assets = await api.getAssetsDirect();
    state.universe = buildUniverse(state.assets);
    populateSelect("STOCK");
    await loadWatchlist();
  } catch (error) {
    setHTML("watchlistStatus", `<div class="status-banner">Unable to initialize watchlist page: ${error.message}</div>`);
  }
}

document.getElementById("watchlistType")?.addEventListener("change", (event) => populateSelect(event.target.value));
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

init();
