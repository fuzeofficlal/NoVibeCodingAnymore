# REST API 接口文档 (Interface Summary)

本项目的微服务架构采用 API Gateway (网关) 统一对外暴露 `/api/v1` 路由，智能分流到底层不同的微服务处理。
- **Go Gateway (主入口)**: `http://localhost:8090`
- **Java Spring Boot (核心业务)**: 内部运行于 `8080` 端口
- **Python FastAPI (数据管线)**: 内部运行于 `8000` 端口

所有的前端/客户端请求，**只需要请求网关 (:8090)** 即可。

---

## 📈 金融市场数据接口群 (转发至 Python 微服务)
这些接口专门负责金融数据的抓取、同步和量化计算。

### 1. 强制数据全量同步
- **Endpoint**: `POST /api/v1/market/sync`
- **描述**: 手动触发一个后台 ETL 任务，进行历史数据追赶和实时数据刷新。
- **返回体示例**:
  ```json
  {"status": "accepted", "message": "Background ETL Sync Started."}
  ```

### 2. 获取实时资产快照价格
- **Endpoint**: `GET /api/v1/market/prices`
- **Query 参数**: `tickers` (可选) - 逗号分隔的资产代码，如 `?tickers=AAPL,BTC-USD`。为空则返回全量。
- **描述**: 从 `market_price` 数据库中极速返回最近一次轮询的强力快照。
- **返回体示例**:
  ```json
  [
    {"ticker_symbol": "AAPL", "current_price": 155.0, "last_updated": "2026-03-31T10:00:00"}
  ]
  ```

### 3. 获取个股历史收盘价数据
- **Endpoint**: `GET /api/v1/market/history/{ticker}`
- **Query 参数**: `start_date` (YYYY-MM-DD), `end_date` (YYYY-MM-DD)
- **描述**: 查询某个资产的时间序列数据集。
- **返回体示例**:
  ```json
  [
    {"trade_date": "2026-03-30", "close_price": 150.00}
  ]
  ```

### 4. 获取量化指标 (简单移动平均线 SMA)
- **Endpoint**: `GET /api/v1/market/indicators/sma/{ticker}`
- **Query 参数**: `days` - 均线窗口天数 (比如 `20`, `50`, `200`)
- **描述**: 实时生成基于时间序列的量化平滑因子。

### 5. 高频实时行情瀑布流 (WebSocket)
- **Endpoint**: `ws://localhost:8090/api/v1/market/ws`
- **连接方式**: 前端直接使用原生 `WebSocket` 对象连接该 URL。Go 网关会自动升级 HTTP 握手。
- **频次规则**: 连接成功后，服务器会持续下发最新资产挂牌价格包。更新频次依赖于后台实际抓取情况（约 1-5 分钟跳变一次）。
- **返回值资产标识**: 新增了 `ASHARE` (A股) 资产大类，以便前端独立切分颜色主题（A股默认红涨绿跌，中美颜色机制相反）。
- **Event 返回体示例**:
  ```json
  {
    "timestamp": 1718294021000,
    "prices": {
      "600519.SS": 1502.50,
      "BTC-USD": 65000.12,
      "AAPL": 189.45
    }
  }
  ```

### 6. 实时期刊突发新闻抓取 (YFinance News)
- **Endpoint**: `GET /api/v1/market/news`
- **Query 参数**: `tickers` - 证券代码，支持逗号分隔批量抓取 (如 `?tickers=AAPL,TSLA`)
- **描述**: 基于 Python 后台多线程穿透 YFinance SDK，无状态抓取标的公司过去几个小时内的全球金融头条，轻度清洗后提供给前端瀑布流或供大模型做风控上下文。
- **返回体示例**:
  ```json
  {
    "AAPL": [
      {"title": "Apple to launch new AI MacBooks", "publisher": "Bloomberg", "link": "https://..."}
    ]
  }
  ```

---

## 💼 投资组合与资金业务群 (转发至 Java 微服务)
这些接口涵盖了用户的核心资产管理、交易记录、以及盈亏 (PnL) 结算。

### 5A. 开设新组合 (开户)
- **Endpoint**: `POST /api/v1/portfolios`
- **描述**: 创建一个新的资产组合。系统强制要求在开户时**必须**存入初始开户本金（`initialDeposit` 参数必填且必须大于0）。
- **Request Body**:
  ```json
  {
    "name": "My Quant Fund",
    "initialDeposit": 50000.00
  }
  ```
- **返回体示例**:
  ```json
  {"portfolioId": "user_a1b2c3d4e5", "name": "My Quant Fund", "cashBalance": 100000.0}
  ```

### 5B. 历史交易账单明细
- **Endpoint**: `GET /api/v1/portfolios/{portfolioId}/transactions`
- **描述**: 按时间倒序拉取该账户产生过的所有交易流水记录（入金、出金、买入、卖出）。

### 5C. 基础组合信息
- **Endpoint**: `GET /api/v1/portfolios/{portfolioId}`
- **描述**: 获取该账户的基本信息和当前现金余量 (Cash Balance)。

### 6. 发起交易/资金流水 (核心事务流)
- **Endpoint**: `POST /api/v1/portfolios/{portfolioId}/transactions`
- **描述**: 发起入金、出金、买入、卖出行为，自动更新现金和头寸表。
- **Request Body (买入示例)**:
  ```json
  {
    "transactionType": "BUY",
    "tickerSymbol": "AAPL",
    "quantity": 100.0,
    "pricePerUnit": 5.00
  }
  ```

### 7. 组合总市值与 PnL 分析汇总
- **Endpoint**: `GET /api/v1/portfolios/{portfolioId}/summary`
- **描述**: Java 会跨微服务读取最新快照，计算整个账号的总资产(Net Asset Value)、总回报率(ROI，百分比)以及资产配置分布。

### 8. 所有持仓列表 (Holdings)
- **Endpoint**: `GET /api/v1/portfolios/{portfolioId}/holdings`
- **描述**: 返回每笔持仓的数量、成本均价，以及通过读取最新快照计算出的当前浮动盈亏（Unrealized PnL）。

### 9. 组合历史业绩时光机
- **Endpoint**: `GET /api/v1/portfolios/{portfolioId}/performance`
- **Query 参数**: `range` - 回溯时间范围 (缺省为 `1M`。可选: `1M`, `3M`, `6M`, `YTD`, `1Y`)
- **描述**: 根据持仓明细映射历史价格数据，每日回放生成资产的历史净值曲线。

### 10A. 量化智投大模型分析报告 (AI Risk Insight)
- **Endpoint**: `GET /api/v1/advisor/{portfolioId}/insight`
- **Request Headers**: `X-API-Key` (必填) - 平台已取消硬编码全局密钥，此请求必须要携带用户的合法大模型 API Key，否则将被拦截。
- **描述**: 基于 Spring AI 调用后台绑定的大语言模型 (`gemini-3-pro-preview` 或 `claude-opus-4-6`)，由其扮演的高级基金量化风险师动态审视 `holdings` 与 `cashBalance`，并利用大模型生成包含结构化风控指引与极端压力测试建议的高质量研报。

### 10B. 自由搜索框与 Agent 自动驾驶中枢 (NL-to-API Chat)
- **Endpoint**: `POST /api/v1/advisor/{portfolioId}/chat`
- **Request Headers**: `X-API-Key` (必填)
- **描述**: 直接对系统说人话！大模型在后台拥有查持仓、查新闻甚至 **直接买卖股票** 的 `Function Calling` (工具调用) 能力。它会自己推断参数去触发底层微服务并返回最终的人类语言结果。
- **Request Body 示例**:
  ```json
  { "query": "帮我看看苹果最近有什么新闻，然后帮我买10股AAPL" }
  ```
- **返回体**: 大模型通过反复挂载工具链，执行完毕后最终返回的文本报告。

### 11A. 全局可用资产列表
- **Endpoint**: `GET /api/v1/assets`
- **描述**: 获取平台配置的所有可交易对象（如中美 500 强、加密货币等列表）。

### 11B. 获取本地数据库资产历史快照 (Java Wrapper)
- **Endpoint**: `GET /api/v1/assets/{symbol}/history`
- **描述**: 直接从中心化 Java 核心 MySQL 数据库 (`history_market_price` 表) 中提取历史时间轴价格曲线。与 Python 直连 Yahoo 的 `/api/v1/market/history/{ticker}` 形成双活机制。


### 12. 自选关注股票池 (Watchlist)
- **Endpoint**: `GET|POST /api/v1/portfolios/{portfolioId}/watchlist` / `DELETE /api/v1/portfolios/{portfolioId}/watchlist/{ticker}`
- **描述**: 管理该投资组合名下的自选股票列表。此处的关注列表将在请求 AI 分析报告时，与实时新闻一并喂给大模型以诊断潜在的建仓风险。
- **POST Body示例**:
  ```json
  { "tickerSymbol": "TSLA" }
  ```

### 13. 个股监控警报阈值 (Price Alerts)
- **Endpoint**: `GET|POST /api/v1/portfolios/{portfolioId}/alerts` / `DELETE /api/v1/portfolios/{portfolioId}/alerts/{alertId}`
- **描述**: 支持直接设立个股报警策略（目前限后端直接落库），包含 `STOP_LOSS` 和 `TAKE_PROFIT`。
- **POST Body示例**:
  ```json
  { "tickerSymbol": "AAPL", "targetPrice": 200.0, "alertType": "TAKE_PROFIT" }
  ```
