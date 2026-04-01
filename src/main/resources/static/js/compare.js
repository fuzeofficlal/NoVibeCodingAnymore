import { api } from "./api.js";
import {
  bindModal,
  buildUniverse,
  drawLineChart,
  formatMoney,
  getPortfolioContext,
  setHTML,
  setText,
  showModal
} from "./ui.js";

const state = {
  assets: [],
  universe: { STOCK: [], BOND: [], CRYPTO: [] },
  range: "MONTH"
};

const rangeMap = {
  DAY: 1,
  WEEK: 7,
  MONTH: 30,
  YEAR: 365
};

function dateOffset(days) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function populateComparators(type) {
  const options = (state.universe[type] || []).map((ticker) => `<option value="${ticker}">${ticker}</option>`).join("");
  document.getElementById("compareA").innerHTML = options;
  document.getElementById("compareB").innerHTML = options;
  if ((state.universe[type] || []).length > 1) {
    document.getElementById("compareB").selectedIndex = 1;
  }
}

async function loadComparison() {
  const left = document.getElementById("compareA").value;
  const right = document.getElementById("compareB").value;
  if (!left || !right || left === right) {
    showModal("Select two assets", "Choose two different stocks, bonds, or crypto symbols to compare.", "error");
    return;
  }

  const days = rangeMap[state.range];
  const startDate = dateOffset(days);
  const endDate = today();

  try {
    const [leftSeries, rightSeries] = await Promise.all([
      api.getAssetHistoryDirect(left),
      api.getAssetHistoryDirect(right)
    ]);

    const filteredLeft = leftSeries.filter((item) => {
      const value = (item.trade_date || item.timestamp || "").slice(0, 10);
      return value >= startDate && value <= endDate;
    });
    const filteredRight = rightSeries.filter((item) => {
      const value = (item.trade_date || item.timestamp || "").slice(0, 10);
      return value >= startDate && value <= endDate;
    });

    drawLineChart(document.getElementById("compareChart"), [
      {
        label: left,
        color: "#b78a56",
        data: filteredLeft.map((item) => ({ x: item.trade_date || item.timestamp, y: Number(item.close_price || item.closePrice) }))
      },
      {
        label: right,
        color: "#8798ad",
        data: filteredRight.map((item) => ({ x: item.trade_date || item.timestamp, y: Number(item.close_price || item.closePrice) }))
      }
    ]);

    const latestLeft = filteredLeft[filteredLeft.length - 1];
    const latestRight = filteredRight[filteredRight.length - 1];
    setText("compareLatestA", latestLeft ? formatMoney(latestLeft.close_price || latestLeft.closePrice) : "--");
    setText("compareLatestB", latestRight ? formatMoney(latestRight.close_price || latestRight.closePrice) : "--");
    setText("compareRangeLabel", `${days} day window`);
  } catch (error) {
    setHTML("compareStatus", `<div class="status-banner">Unable to load comparison data: ${error.message}</div>`);
  }
}

async function init() {
  bindModal();
  try {
    state.assets = await api.getAssetsDirect();
    state.universe = buildUniverse(state.assets);
    populateComparators("STOCK");
    await loadComparison();
  } catch (error) {
    setHTML("compareStatus", `<div class="status-banner">Unable to initialize compare page: ${error.message}</div>`);
  }
}

document.querySelectorAll("[data-range]").forEach((button) => {
  button.addEventListener("click", async () => {
    document.querySelectorAll("[data-range]").forEach((node) => node.classList.remove("active"));
    button.classList.add("active");
    state.range = button.dataset.range;
    await loadComparison();
  });
});

document.getElementById("compareType")?.addEventListener("change", (event) => populateComparators(event.target.value));
document.getElementById("compareForm")?.addEventListener("submit", (event) => {
  event.preventDefault();
  loadComparison();
});

init();
