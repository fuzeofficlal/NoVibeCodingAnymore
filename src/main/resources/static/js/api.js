const host = window.location.hostname || "localhost";
const protocol = window.location.protocol === "https:" ? "https:" : "http:";
const wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:";

export const API_BASE = `${protocol}//${host}:8090/api/v1`;
export const WS_URL = `${wsProtocol}//${host}:8090/api/v1/market/ws`;

async function request(path, options = {}, base = API_BASE) {
  const response = await fetch(`${base}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    let message = `Request failed: ${response.status}`;
    try {
      const data = await response.json();
      message = data.message || data.error || JSON.stringify(data);
    } catch {
      const text = await response.text();
      if (text) message = text;
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  return response.text();
}

async function getLatestAssetPrice(ticker) {
  const series = await request(`/assets/${encodeURIComponent(ticker)}/history`);
  const latest = series[series.length - 1];
  const previous = series[series.length - 2] || latest;
  return {
    ticker,
    current_price: Number(latest?.closePrice || latest?.close_price || 0),
    previous_price: Number(previous?.closePrice || previous?.close_price || latest?.closePrice || latest?.close_price || 0)
  };
}

export const api = {
  getAssets: () => request("/assets"),
  getAssetsDirect: () => request("/assets"),
  getPrices: async (tickers = [], force = false) => {
    try {
      let endpoint = '/market/prices';
      const query = [];
      if (tickers.length) query.push(`tickers=${encodeURIComponent(tickers.join(","))}`);
      if (force) query.push('force=true');
      if (query.length) endpoint += `?${query.join("&")}`;
      return await request(endpoint);
    } catch {
      return Promise.all(tickers.map((ticker) => getLatestAssetPrice(ticker)));
    }
  },
  getMarketHistory: async (ticker, startDate, endDate) => {
    try {
      return await request(`/market/history/${encodeURIComponent(ticker)}?start_date=${startDate}&end_date=${endDate}`);
    } catch {
      const series = await request(`/assets/${encodeURIComponent(ticker)}/history`);
      return series.filter((item) => {
        const value = (item.trade_date || item.timestamp || "").slice(0, 10);
        return value >= startDate && value <= endDate;
      });
    }
  },
  getAssetHistoryDirect: (ticker) => request(`/assets/${encodeURIComponent(ticker)}/history`),
  getAssetHistoryBatch: async (tickers = []) => {
    const results = await Promise.all(tickers.map(async (ticker) => [ticker, await request(`/assets/${encodeURIComponent(ticker)}/history`)]));
    return Object.fromEntries(results);
  },
  getPortfolio: (portfolioId) => request(`/portfolios/${portfolioId}`),
  getPortfolios: () => request("/portfolios"),
  createPortfolio: (payload) => request("/portfolios", { method: "POST", body: JSON.stringify(payload) }),
  getSummary: (portfolioId) => request(`/portfolios/${portfolioId}/summary`),
  getHoldings: (portfolioId) => request(`/portfolios/${portfolioId}/holdings`),
  getTransactions: (portfolioId) => request(`/portfolios/${portfolioId}/transactions`),
  getPerformance: (portfolioId, range = "1M") => request(`/portfolios/${portfolioId}/performance?range=${range}`),
  processTransaction: (portfolioId, payload) =>
    request(`/portfolios/${portfolioId}/transactions`, { method: "POST", body: JSON.stringify(payload) }),
  getWatchlist: (portfolioId) => request(`/portfolios/${portfolioId}/watchlist`),
  addWatchlist: (portfolioId, payload) =>
    request(`/portfolios/${portfolioId}/watchlist`, { method: "POST", body: JSON.stringify(payload) }),
  removeWatchlist: (portfolioId, ticker) =>
    request(`/portfolios/${portfolioId}/watchlist/${encodeURIComponent(ticker)}`, { method: "DELETE" }),
  getMarketNews: async (tickers = []) => {
    try {
      return await request(`/market/news?tickers=${encodeURIComponent(tickers.join(","))}`);
    } catch {
      return {};
    }
  },
  getInsight: (portfolioId, apiKey) => {
    const headers = apiKey ? { "X-API-Key": apiKey } : {};
    return request(`/advisor/${portfolioId}/insight`, { headers });
  },
  chatWithAdvisor: (portfolioId, apiKey, query) => {
    const headers = apiKey ? { "X-API-Key": apiKey } : {};
    return request(`/advisor/${portfolioId}/chat`, {
      method: "POST",
      headers,
      body: JSON.stringify({ query })
    });
  },
  getAlerts: (portfolioId) => request(`/portfolios/${portfolioId}/alerts`),
  addAlert: (portfolioId, payload) => request(`/portfolios/${portfolioId}/alerts`, { method: "POST", body: JSON.stringify(payload) }),
  removeAlert: (portfolioId, alertId) => request(`/portfolios/${portfolioId}/alerts/${alertId}`, { method: "DELETE" }),
  getSma: (ticker, days = 50) => request(`/market/indicators/sma/${encodeURIComponent(ticker)}?days=${days}`),
  syncMarket: () => request(`/market/sync`, { method: "POST" })
};

export function openMarketSocket(onMessage, onError) {
  const urls = [WS_URL];
  let activeSocket = null;

  const connect = (index) => {
    const socket = new WebSocket(urls[index]);
    activeSocket = socket;
    socket.onmessage = (event) => {
      try {
        onMessage(JSON.parse(event.data));
      } catch (error) {
        onError?.(error);
      }
    };
    socket.onerror = () => {
      if (index < urls.length - 1) {
        socket.close();
        connect(index + 1);
        return;
      }
      onError?.(new Error("All market websocket endpoints failed."));
    };
  };

  connect(0);
  return {
    close() {
      activeSocket?.close();
    }
  };
}
