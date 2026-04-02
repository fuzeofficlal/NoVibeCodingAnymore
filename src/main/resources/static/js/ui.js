const portfolioKey = "novibe.currentPortfolioId";
const portfolioNameKey = "novibe.currentPortfolioName";
const apiKeyStore = "novibe.advisorApiKey";

export const curatedUniverse = {
  STOCK: ["AAPL", "MSFT", "NVDA", "TSLA", "AMZN", "META"],
  BOND: ["TLT", "IEF", "SHY", "LQD", "HYG", "BND"],
  CRYPTO: ["BTC-USD", "ETH-USD", "SOL-USD", "XRP-USD", "DOGE-USD", "ADA-USD"],
  ASHARE: ["600519.SS", "000001.SZ", "601398.SS", "300750.SZ"]
};

export function savePortfolioContext(id, name = "") {
  localStorage.setItem(portfolioKey, id);
  if (name) localStorage.setItem(portfolioNameKey, name);
}

export function getPortfolioContext() {
  return {
    id: localStorage.getItem(portfolioKey) || "",
    name: localStorage.getItem(portfolioNameKey) || ""
  };
}

export function clearPortfolioContext() {
  localStorage.removeItem(portfolioKey);
  localStorage.removeItem(portfolioNameKey);
}

export function saveAdvisorKey(key) {
  localStorage.setItem(apiKeyStore, key);
}

export function getAdvisorKey() {
  return localStorage.getItem(apiKeyStore) || "";
}

export function formatMoney(value, options = {}) {
  const number = Number(value || 0);
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: options.currency || "USD",
    maximumFractionDigits: options.maximumFractionDigits ?? 2
  }).format(number);
}

export function formatPercent(value) {
  const number = Number(value || 0);
  return `${number >= 0 ? "+" : ""}${number.toFixed(2)}%`;
}

export function formatQuantity(value) {
  return Number(value || 0).toLocaleString("en-US", { maximumFractionDigits: 4 });
}

export function shortDate(value) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

export function fullDate(value) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

export function setText(id, value) {
  const node = document.getElementById(id);
  if (node) node.textContent = value;
}

export function setHTML(id, value) {
  const node = document.getElementById(id);
  if (node) node.innerHTML = value;
}

export function toast(message, type = "success") {
  const stack = document.getElementById("toastStack");
  if (!stack) return;
  const item = document.createElement("div");
  item.className = `toast ${type}`;
  item.textContent = message;
  stack.appendChild(item);
  window.setTimeout(() => item.remove(), 3400);
}

export function showModal(title, body, tone = "success") {
  const backdrop = document.getElementById("appModal");
  if (!backdrop) return;
  const titleNode = backdrop.querySelector("[data-modal-title]");
  const bodyNode = backdrop.querySelector("[data-modal-body]");
  const panel = backdrop.querySelector(".modal");
  titleNode.textContent = title;
  
  if (tone === "error") {
    let html = body
      .replace(/</g, "&lt;").replace(/>/g, "&gt;") // sanitize HTML tags
      .replace(/\*\*(.*?)\*\*/g, '<strong style="color: #ff5a5f;">$1</strong>') // Bold text with red emphasis
      .replace(/\*(.*?)\*/g, '<em>$1</em>') // Italic
      .replace(/={10,}/g, '<hr style="border:0; border-top:1px dashed rgba(255,90,95,0.4); margin: 12px 0;">') // Separators
      .replace(/^\*\s+(.*)$/gm, '<div style="margin-left: 12px;"><span style="color:#ff5a5f; margin-right: 6px;">•</span>$1</div>') // Bullets
      .replace(/\n\n/g, '<br><br>') // Paragraphs
      .replace(/\n(?!\<)/g, '<br>') // Single line breaks (not preceded by our html tags like <br> or <div>)
      .replace(/🚨/g, '<span style="font-size: 1.25em">🚨</span>');
    bodyNode.innerHTML = html;
  } else {
    bodyNode.textContent = body;
  }
  
  panel.style.borderColor = tone === "error" ? "rgba(255, 90, 95, 0.28)" : "rgba(40, 209, 124, 0.28)";
  backdrop.classList.add("visible");
}

export function bindModal() {
  const backdrop = document.getElementById("appModal");
  if (!backdrop) return;
  backdrop.addEventListener("click", (event) => {
    if (event.target === backdrop || event.target.hasAttribute("data-close-modal")) {
      backdrop.classList.remove("visible");
    }
  });
}

export function renderMarketCards(element, assets, previous = {}) {
  if (!element) return;
  element.innerHTML = assets.map((asset) => {
    const current = Number(asset.currentPrice || 0);
    const prev = Number(asset.previousPrice ?? previous[asset.ticker] ?? current);
    const delta = current - prev;
    const pct = prev ? (delta / prev) * 100 : 0;
    const tone = delta >= 0 ? "positive" : "negative";
    const spark = Array.isArray(asset.sparkline) && asset.sparkline.length > 1 ? buildSparkline(asset.sparkline, delta >= 0 ? "#28d17c" : "#ff5a5f") : "";
    return `
      <article class="quote-card interactive" title="${asset.name || asset.ticker}">
        <div class="quote-header">
          <div>
            <div class="ticker">${asset.ticker}</div>
            <div class="muted">${asset.name || asset.type}</div>
          </div>
          <span class="asset-tag">${asset.type}</span>
        </div>
        <div class="quote-price">${formatMoney(current)}</div>
        <div class="quote-change ${tone}">${delta >= 0 ? "+" : ""}${delta.toFixed(2)} / ${formatPercent(pct)}</div>
        ${spark}
      </article>
    `;
  }).join("");
}

function buildSparkline(points, color) {
  if (!points.length) return "";
  const min = Math.min(...points);
  const max = Math.max(...points);
  const spread = max - min || 1;
  const width = 180;
  const height = 42;
  const path = points.map((value, index) => {
    const x = (index / Math.max(points.length - 1, 1)) * width;
    const y = height - ((value - min) / spread) * height;
    return `${index === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`;
  }).join(" ");
  return `
    <svg class="sparkline" viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true">
      <path d="${path}" fill="none" stroke="${color}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"></path>
    </svg>
  `;
}

export function renderAllocation(element, allocation = {}, totalValue = 0) {
  if (!element) return;
  const entries = Object.entries(allocation || {}).filter(([, value]) => Number(value) > 0);
  if (!entries.length) {
    element.innerHTML = `<div class="empty-state">No allocation yet. Fund the account and place a trade to start building the portfolio mix.</div>`;
    return;
  }

  const colors = ["#b78a56", "#8798ad", "#cfac7e", "#ff5a5f", "#f6c45b", "#5f6f84"];
  let running = 0;
  const stops = entries.map(([label, value], index) => {
    const percent = totalValue ? (Number(value) / Number(totalValue)) * 100 : 0;
    const start = running;
    running += percent;
    return { label, value: Number(value), percent, color: colors[index % colors.length], start, end: running };
  });

  const gradient = stops.map((item) => `${item.color} ${item.start}% ${item.end}%`).join(", ");

  element.innerHTML = `
    <div class="allocation-wrap">
      <div class="allocation-donut" style="background: conic-gradient(${gradient});">
        <div class="allocation-center">
          <div>
            <div class="label">Allocated</div>
            <div class="ticker">${formatMoney(totalValue)}</div>
          </div>
        </div>
      </div>
      <div class="allocation-list">
        ${stops.map((item) => `
          <div class="allocation-item">
            <span class="swatch" style="background:${item.color}"></span>
            <div class="allocation-bar"><span style="width:${Math.max(item.percent, 2)}%;background:${item.color};"></span></div>
            <div>${item.label} ${item.percent.toFixed(1)}%</div>
          </div>
        `).join("")}
      </div>
    </div>
  `;
}

export function renderAllocationBreakdown(element, allocation = {}, totalValue = 0) {
  if (!element) return;
  const rows = ["CASH", "STOCK", "BOND", "CRYPTO"].map((label) => {
    const value = Number(allocation[label] || 0);
    const percent = totalValue ? (value / Number(totalValue)) * 100 : 0;
    return { label, value, percent };
  });
  element.innerHTML = rows.map((item) => `
    <div class="allocation-stat">
      <div class="row-between">
        <strong>${item.label}</strong>
        <span>${formatMoney(item.value)}</span>
      </div>
      <div class="allocation-bar"><span style="width:${Math.max(item.percent, item.value > 0 ? 4 : 0)}%;background:${allocationColor(item.label)};"></span></div>
      <div class="muted">${item.percent.toFixed(1)}% of NAV</div>
    </div>
  `).join("");
}

function allocationColor(label) {
  const colors = {
    CASH: "#b78a56",
    STOCK: "#8798ad",
    ASHARE: "#d94c4c",
    BOND: "#cfac7e",
    CRYPTO: "#5f6f84"
  };
  return colors[label] || "#b78a56";
}

export function drawLineChart(canvas, seriesList = [], options = {}) {
  if (!canvas) return;
  const shell = canvas.parentElement;
  let tooltip = shell.querySelector(".chart-tooltip");
  if (!tooltip) {
    tooltip = document.createElement("div");
    tooltip.className = "chart-tooltip";
    shell.appendChild(tooltip);
  }

  const ctx = canvas.getContext("2d");
  const dpr = window.devicePixelRatio || 1;
  const width = canvas.clientWidth || 800;
  const height = canvas.clientHeight || 340;
  canvas.width = Math.floor(width * dpr);
  canvas.height = Math.floor(height * dpr);
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, width, height);

  const padding = { top: 18, right: 16, bottom: 40, left: 56 };
  const innerWidth = width - padding.left - padding.right;
  const innerHeight = height - padding.top - padding.bottom;
  const points = seriesList.flatMap((series) => series.data.map((item) => ({ ...item, color: series.color, label: series.label })));

  if (!points.length) {
    ctx.fillStyle = "#8b97ab";
    ctx.font = "14px IBM Plex Sans";
    ctx.fillText("No chart data available.", padding.left, height / 2);
    return;
  }

  const xValues = points.map((item) => new Date(item.x).getTime());
  const yValues = points.map((item) => Number(item.y));
  const minX = Math.min(...xValues);
  const maxX = Math.max(...xValues);
  const minY = Math.min(...yValues);
  const maxY = Math.max(...yValues);
  const safeMaxY = maxY === minY ? maxY + 1 : maxY;
  const safeMaxX = maxX === minX ? maxX + 1 : maxX;

  const scaleX = (value) => padding.left + ((new Date(value).getTime() - minX) / (safeMaxX - minX)) * innerWidth;
  const scaleY = (value) => padding.top + innerHeight - ((Number(value) - minY) / (safeMaxY - minY)) * innerHeight;

  const interactivePoints = [];
  drawLineChartStatic(ctx, canvas, seriesList, {
    padding,
    width,
    height,
    minX,
    safeMaxX,
    minY,
    safeMaxY,
    innerHeight,
    innerWidth,
    interactivePoints
  });

  canvas.onmousemove = (event) => {
    const bounds = canvas.getBoundingClientRect();
    const mouseX = event.clientX - bounds.left;
    const mouseY = event.clientY - bounds.top;
    let nearest = null;
    let distance = Infinity;
    interactivePoints.forEach((point) => {
      const delta = Math.abs(point.x - mouseX);
      if (delta < distance) {
        distance = delta;
        nearest = point;
      }
    });
    if (!nearest || distance > 42) {
      tooltip.classList.remove("visible");
      drawLineChartStatic(ctx, canvas, seriesList, {
        padding,
        width,
        height,
        minX,
        safeMaxX,
        minY,
        safeMaxY,
        innerHeight,
        innerWidth
      });
      return;
    }

    const sharedPoints = interactivePoints.filter((point) => Math.abs(point.x - nearest.x) < 6);
    drawLineChartStatic(ctx, canvas, seriesList, { padding, width, height, minX, safeMaxX, minY, safeMaxY, innerHeight, innerWidth });
    ctx.beginPath();
    ctx.strokeStyle = "rgba(255, 213, 111, 0.32)";
    ctx.lineWidth = 1;
    ctx.moveTo(nearest.x, padding.top);
    ctx.lineTo(nearest.x, height - padding.bottom);
    ctx.stroke();
    sharedPoints.forEach((point) => {
      ctx.beginPath();
      ctx.fillStyle = point.color;
      ctx.arc(point.x, point.y, 4.8, 0, Math.PI * 2);
      ctx.fill();
    });

    tooltip.innerHTML = `<strong>${shortDate(nearest.raw.x)}</strong><br>${sharedPoints.map((point) => `<span style="color:${point.color}">${point.label}</span>: ${formatMoney(point.raw.y)}`).join("<br>")}`;
    tooltip.style.left = `${nearest.x}px`;
    tooltip.style.top = `${Math.min(...sharedPoints.map((point) => point.y))}px`;
    tooltip.classList.add("visible");
  };

  canvas.onmouseleave = () => {
    tooltip.classList.remove("visible");
    drawLineChartStatic(ctx, canvas, seriesList, {
      padding,
      width,
      height,
      minX,
      safeMaxX,
      minY,
      safeMaxY,
      innerHeight,
      innerWidth
    });
  };
}

function drawLineChartStatic(ctx, canvas, seriesList, meta) {
  const { width, height, padding, innerHeight, innerWidth, minX, safeMaxX, minY, safeMaxY, interactivePoints } = meta;
  ctx.clearRect(0, 0, width, height);
  ctx.strokeStyle = "rgba(255,255,255,0.08)";
  ctx.lineWidth = 1;
  for (let i = 0; i <= 4; i += 1) {
    const y = padding.top + (innerHeight / 4) * i;
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(width - padding.right, y);
    ctx.stroke();
  }
  ctx.fillStyle = "#8b97ab";
  ctx.font = "12px IBM Plex Mono";
  for (let i = 0; i <= 4; i += 1) {
    const value = minY + ((safeMaxY - minY) / 4) * (4 - i);
    const y = padding.top + (innerHeight / 4) * i;
    ctx.fillText(formatMoney(value), 4, y + 4);
  }
  const scaleX = (value) => padding.left + ((new Date(value).getTime() - minX) / (safeMaxX - minX)) * innerWidth;
  const scaleY = (value) => padding.top + innerHeight - ((Number(value) - minY) / (safeMaxY - minY)) * innerHeight;
  const longest = seriesList.reduce((acc, series) => Math.max(acc, series.data.length), 0);
  const tickCount = Math.min(5, Math.max(longest, 2));
  for (let i = 0; i < tickCount; i += 1) {
    const ratio = tickCount === 1 ? 0 : i / (tickCount - 1);
    const value = minX + (safeMaxX - minX) * ratio;
    const x = padding.left + innerWidth * ratio;
    ctx.fillText(shortDate(value), x - 18, height - 12);
  }
  seriesList.forEach((series) => {
    ctx.beginPath();
    series.data.forEach((item, index) => {
      const x = scaleX(item.x);
      const y = scaleY(item.y);
      if (index === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
      interactivePoints?.push({ x, y, raw: item, color: series.color, label: series.label });
    });
    ctx.strokeStyle = series.color;
    ctx.lineWidth = 2.2;
    ctx.stroke();
  });
}

export function groupAssets(assetList = []) {
  const map = new Map();
  assetList.forEach((asset) => map.set(asset.symbol, asset));
  return map;
}

export function buildUniverse(assetList = []) {
  const byType = { STOCK: [], BOND: [], CRYPTO: [], ASHARE: [] };
  assetList.forEach((asset) => {
    if (byType[asset.type] !== undefined) byType[asset.type].push(asset.symbol);
  });
  Object.keys(byType).forEach((type) => {
    if (!byType[type].length) byType[type] = curatedUniverse[type];
  });
  return byType;
}

function listenForAiAlerts() {
  const host = window.location.hostname || "localhost";
  const protocol = window.location.protocol === "https:" ? "https:" : "http:";
  const eventSource = new window.EventSource(`${protocol}//${host}:8090/api/v1/advisor/stream`);
  
  eventSource.addEventListener("alert", (event) => {
    try {
      const message = decodeURIComponent(escape(atob(event.data)));
      showModal("\uD83D\uDEA8 AI Copilot Alert", message, "error");
    } catch (e) {
      console.error("Failed to decode AI alert", e);
    }
  });
  eventSource.onerror = () => {
    eventSource.close();
    setTimeout(listenForAiAlerts, 10000); // Retry after 10 seconds
  };
}

if (window.EventSource) {
  setTimeout(listenForAiAlerts, 1000);
}
