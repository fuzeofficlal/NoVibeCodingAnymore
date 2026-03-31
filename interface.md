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
- **描述**: 通过 Python 原生的 Pandas 实时生成基于时间序列的量化平滑因子。

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

### 10. 全局可用资产列表
- **Endpoint**: `GET /api/v1/assets`
- **描述**: 获取平台配置的所有可交易对象（如美股 500 强、加密货币等列表）。
