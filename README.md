[表1: Company Info] (公司信息/股票基础表)
| PK: ticker_symbol
|
|---------------------+-----------------------+
| 1:N                 | 1:N                   | 1:N
v                     v                       v
[表2: Market Data]    [表4: Position]         [表5: Transaction]
(历史市场价格记录)    (当前持仓快照)          (交易流水/资金动作)
PK: ticker_symbol     PK: position_id         PK: transaction_id
PK: timestamp         FK: ticker_symbol       FK: ticker_symbol (存取款可为空)
FK: portfolio_id        FK: portfolio_id
|                       |
| N:1                   | N:1
+-----------+-----------+
|
v
[表3: Portfolio] (总投资组合表)
PK: portfolio_id
cash_balance (现金余额)