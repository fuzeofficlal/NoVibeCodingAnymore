# REST API 接口文档 (Interface Summary)

本文档总结了系统当前支持的所有全后端对接 API 接口。前端开发者可依据此文档进行数据联调。

---

## 模块一：投资组合与交易核心 (Portfolio & Transactions)
> **职责**：负责所有涉及金额、持仓变动的“写”操作及核心对账“读”操作。

### 1. 执行资金流转与资产交易
- **URL**: `POST /api/v1/portfolios/{portfolioId}/transactions`
- **说明**: 前端执行买入、卖出股票，或充值、提现等核心操作统一经由此接口。
- **Request Body**:
  ```json
  {
    "tickerSymbol": "AAPL",      // 若为纯资金存取(DEPOSIT/WITHDRAW)，传 null
    "transactionType": "BUY",    // 可选值: BUY, SELL, DEPOSIT, WITHDRAW
    "quantity": 10.0,            // 存款或取款时，金额数量填在这个字段
    "pricePerUnit": 150.00       // 若为纯资金存取，传 1.0 即可
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "message": "Transaction successful",
    "transactionId": "txn-8f2e2...",
    "newCashBalance": 3500.00
  }
  ```
- **异常提示**: 余额不足(InsufficientFunds)、股票不足(InsufficientStock) 会在请求体校验后抛出异常。

### 2. 获取投资组合基础信息
- **URL**: `GET /api/v1/portfolios/{portfolioId}`
- **说明**: 用于快速获取投资组合名称及可用现金。
- **Response (200 OK)**:
  ```json
  {
    "portfolioId": "port-001",
    "name": "My Retirement Fund",
    "cashBalance": 5200.00
  }
  ```

---

## 模块二：Dashboard 视图数据 (Analytics)
> **职责**：专为 Dashboard 仪表盘读取并预处理数据，提供图表所需的数据切片。

### 3. 获取顶层指标与资产配置饼图
- **URL**: `GET /api/v1/portfolios/{portfolioId}/summary`
- **说明**: 返回顶部两个核心大数字，以及按类目汇总的资产饼状分配详情。
- **Response (200 OK)**:
  ```json
  {
    "totalPortfolioValue": 152430.25,
    "totalReturnPercentage": 12.8,
    "allocation": {
      "CASH": 5200.00,
      "STOCKS": 110900.20,
      "BONDS": 25330.00,
      "CRYPTO": 11000.05
    }
  }
  ```

### 4. 获取详细持仓列表 (Your Holdings)
- **URL**: `GET /api/v1/portfolios/{portfolioId}/holdings`
- **说明**: 提供明细表格（含标的、持仓份额、当前价、成本均价、总市值以及浮动盈亏）。
- **Response (200 OK)**:
  ```json
  [
    {
      "symbol": "AAPL",
      "companyName": "Apple Inc.",
      "shares": 35,
      "currentPrice": 145.32,
      "costBasis": 120.00,
      "marketValue": 5086.20,
      "pl": 886.20
    }
  ]
  ```

### 5. 获取历史业绩走势曲线
- **URL**: `GET /api/v1/portfolios/{portfolioId}/performance`
- **Params**: `?range=1M` (支持 `1M`, `3M`, `6M`, `YTD`, `1Y`)
- **说明**: 计算指定回溯期内，资产总值每天的变化曲线（基于历史股价与当前持仓进行估算回测）。
- **Response (200 OK)**:
  ```json
  [
    {"date": "2023-04-01", "value": 120500.00},
    {"date": "2023-04-02", "value": 123800.50},
    {"date": "2023-04-03", "value": 122900.00}
  ]
  ```

---

## 模块三：参考数据 (Reference Data)
> **职责**：供前端提取市场元数据字典。

### 6. 获取所有可交易资产列表
- **URL**: `GET /api/v1/assets`
- **说明**: 检索系统中允许交易的所有证券、货币的资产类型字典 (用于 Add Assets 模态框)。
- **Response (200 OK)**:
  ```json
  [
    {"symbol": "AAPL", "name": "Apple Inc.", "type": "STOCK"},
    {"symbol": "US10Y", "name": "US 10-Year Treasury", "type": "BONDS"}
  ]
  ```
