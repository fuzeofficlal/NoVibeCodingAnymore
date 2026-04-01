import { api, openMarketSocket } from "./api.js";
import { bindModal, getPortfolioContext, renderMarketCards, savePortfolioContext, setText, showModal, toast } from "./ui.js";

async function loadHomeMarketBoard() {
  const featured = ["AAPL", "MSFT", "NVDA", "TLT", "IEF", "BND", "BTC-USD", "ETH-USD", "SOL-USD"];
  try {
    const [assets, histories] = await Promise.all([api.getAssets(), api.getAssetHistoryBatch(featured)]);
    const assetMap = new Map(assets.map((asset) => [asset.symbol, asset]));
    const payload = featured.map((ticker) => {
      const series = histories[ticker] || [];
      const latest = series[series.length - 1];
      const previous = series[series.length - 2] || latest;
      return {
        ticker,
        currentPrice: Number(latest?.closePrice || latest?.close_price || 0),
        previousPrice: Number(previous?.closePrice || previous?.close_price || 0),
        sparkline: series.slice(-12).map((item) => Number(item.closePrice || item.close_price || 0)),
        type: assetMap.get(ticker)?.type || (ticker.includes("-USD") ? "CRYPTO" : "STOCK"),
        name: assetMap.get(ticker)?.name || ticker
      };
    });
    renderMarketCards(document.getElementById("homeMarketBoard"), payload);

    openMarketSocket((packet) => {
      const prices = packet.prices || {};
      const nextPayload = payload.map((item) => ({
        ...item,
        previousPrice: item.currentPrice,
        currentPrice: Number(prices[item.ticker] || item.currentPrice)
      }));
      renderMarketCards(document.getElementById("homeMarketBoard"), nextPayload);
    }, () => {});
  } catch {
    const board = document.getElementById("homeMarketBoard");
    if (board) {
      board.innerHTML = `<div class="empty-state">Live market preview is temporarily unavailable. The rest of the terminal can still be used.</div>`;
    }
  }
}

function init() {
  bindModal();
  const existing = getPortfolioContext();
  if (existing.id) {
    setText("resumePortfolioId", existing.id);
    setText("resumePortfolioName", existing.name || "Saved portfolio");
  }

  document.getElementById("resumeButton")?.addEventListener("click", () => {
    window.location.href = existing.id ? "/dashboard.html" : "/trade.html";
  });

  document.getElementById("openAccountForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = document.getElementById("portfolioName").value.trim();
    const initialDeposit = Number(document.getElementById("initialDeposit").value);

    if (!name) {
      showModal("Name required", "Please name the portfolio before opening the account.", "error");
      return;
    }

    if (!(initialDeposit > 0)) {
      showModal("Initial deposit required", "Creating a new portfolio requires an initial deposit greater than 0.", "error");
      return;
    }

    try {
      const created = await api.createPortfolio({ name, initialDeposit });
      savePortfolioContext(created.portfolioId, created.name);
      toast("Portfolio opened successfully.");
      showModal("Account opened", `Portfolio ${created.name} is ready. Initial capital has been deposited successfully.`);
      window.setTimeout(() => {
        window.location.href = "/dashboard.html";
      }, 900);
    } catch (error) {
      showModal("Open account failed", error.message, "error");
    }
  });

  loadHomeMarketBoard();
}

init();
