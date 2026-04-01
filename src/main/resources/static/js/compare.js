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
  const datalistA = document.getElementById("compareAList");
  const datalistB = document.getElementById("compareBList");
  const inputA = document.getElementById("compareA");
  const inputB = document.getElementById("compareB");
  const tickers = state.universe[type] || [];
  const optionsHtml = tickers.map((ticker) => `<option value="${ticker}">${state.assetMap?.get(ticker)?.name || ticker}</option>`).join("");
  datalistA.innerHTML = optionsHtml;
  datalistB.innerHTML = optionsHtml;
  
  if (tickers.length > 0 && !tickers.includes(inputA.value)) inputA.value = tickers[0];
  if (tickers.length > 1 && !tickers.includes(inputB.value)) inputB.value = tickers[1];
}

async function loadComparison() {
  const left = document.getElementById("compareA").value?.trim();
  const right = document.getElementById("compareB").value?.trim();
  if (!left) {
    showModal("Select Asset A", "Please choose at least Asset A to view historical data.", "error");
    return;
  }
  if (left && right && left === right) {
    showModal("Same Assets selected", "Choose two different assets for comparison, or leave Asset B blank.", "error");
    return;
  }

  const days = rangeMap[state.range];
  const startDate = dateOffset(days);
  const endDate = today();

  try {
    const promises = [api.getAssetHistoryDirect(left)];
    if (right) promises.push(api.getAssetHistoryDirect(right));
    
    const results = await Promise.all(promises);
    const leftSeries = results[0];
    const rightSeries = results[1] || [];

    const filteredLeft = leftSeries.filter((item) => {
      const value = (item.trade_date || item.timestamp || "").slice(0, 10);
      return value >= startDate && value <= endDate;
    });
    const filteredRight = rightSeries.filter((item) => {
      const value = (item.trade_date || item.timestamp || "").slice(0, 10);
      return value >= startDate && value <= endDate;
    });

    const chartData = [{
      label: left,
      color: "#b78a56",
      data: filteredLeft.map((item) => ({ x: item.trade_date || item.timestamp, y: Number(item.close_price || item.closePrice) }))
    }];
    
    if (right) {
      chartData.push({
        label: right,
        color: "#8798ad",
        data: filteredRight.map((item) => ({ x: item.trade_date || item.timestamp, y: Number(item.close_price || item.closePrice) }))
      });
    }
    
    drawLineChart(document.getElementById("compareChart"), chartData);

    const latestLeft = filteredLeft[filteredLeft.length - 1];
    setText("compareLatestA", latestLeft ? formatMoney(latestLeft.close_price || latestLeft.closePrice) : "--");
    
    if (right) {
      const latestRight = filteredRight[filteredRight.length - 1];
      setText("compareLatestB", latestRight ? formatMoney(latestRight.close_price || latestRight.closePrice) : "--");
    } else {
      setText("compareLatestB", "N/A");
    }
    setText("compareRangeLabel", `${days} day window`);
  } catch (error) {
    setHTML("compareStatus", `<div class="status-banner">Unable to load comparison data: ${error.message}</div>`);
  }
}

async function init() {
  bindModal();
  try {
    state.assets = await api.getAssetsDirect();
    state.assetMap = new Map(state.assets.map(a => [a.symbol, a]));
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
