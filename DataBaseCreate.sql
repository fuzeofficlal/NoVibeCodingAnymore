-- 1. 创建数据库
CREATE DATABASE newport_db;

-- 切换到新创建的数据库 (MySQL 语法，如果是 PostgreSQL 请使用 \c newport_db)
USE newport_db;

-- ==========================================
-- 第一阶段：创建无外键依赖的【主表】
-- ==========================================

-- 表1：Company Info (公司基础信息表)
CREATE TABLE company_info (
                              ticker_symbol VARCHAR(50) PRIMARY KEY,
                              company_name VARCHAR(255) NOT NULL
);

-- 表3：Portfolio (投资组合主表)
CREATE TABLE portfolio (
                           portfolio_id VARCHAR(50) PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           cash_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00
);


-- ==========================================
-- 第二阶段：创建带有外键依赖的【子表】
-- ==========================================

-- 表2：Market Data (市场历史数据表)
CREATE TABLE market_data (
                             ticker_symbol VARCHAR(50) NOT NULL,
                             timestamp TIMESTAMP NOT NULL,
                             close_price DECIMAL(15, 4) NOT NULL,
                             PRIMARY KEY (ticker_symbol, timestamp),
                             FOREIGN KEY (ticker_symbol) REFERENCES company_info(ticker_symbol)
);

-- 表4：Position (当前持仓表)
CREATE TABLE position (
                          position_id VARCHAR(50) PRIMARY KEY,
                          portfolio_id VARCHAR(50) NOT NULL,
                          ticker_symbol VARCHAR(50) NOT NULL,
                          total_quantity DECIMAL(15, 4) NOT NULL DEFAULT 0,
                          average_cost DECIMAL(15, 4) NOT NULL DEFAULT 0,
                          FOREIGN KEY (portfolio_id) REFERENCES portfolio(portfolio_id),
                          FOREIGN KEY (ticker_symbol) REFERENCES company_info(ticker_symbol)
);

-- 表5：Transaction (交易流水表)
-- 注意：transaction 是 SQL 保留字，因此表名使用 portfolio_transaction
CREATE TABLE portfolio_transaction (
                                       transaction_id VARCHAR(50) PRIMARY KEY,
                                       portfolio_id VARCHAR(50) NOT NULL,
                                       ticker_symbol VARCHAR(50), -- 存取款时允许为空 (NULL)
                                       transaction_type VARCHAR(20) NOT NULL, -- 'BUY', 'SELL', 'DEPOSIT', 'WITHDRAW'
                                       quantity DECIMAL(15, 4) NOT NULL,
                                       price_per_unit DECIMAL(15, 4) NOT NULL DEFAULT 0.00,
                                       transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (portfolio_id) REFERENCES portfolio(portfolio_id),
                                       FOREIGN KEY (ticker_symbol) REFERENCES company_info(ticker_symbol)
);

ALTER TABLE company_info
    ADD COLUMN asset_type VARCHAR(20) NOT NULL DEFAULT 'STOCK';

-- 表6：Market Price 实时 价格
CREATE TABLE market_price (
    ticker_symbol VARCHAR(50) PRIMARY KEY,
    current_price DECIMAL(15, 4) NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (ticker_symbol) REFERENCES company_info(ticker_symbol)
);



-- 表7：Watchlist 股票池
CREATE TABLE watchlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id VARCHAR(50) NOT NULL,
    ticker_symbol VARCHAR(50) NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(portfolio_id),
    FOREIGN KEY (ticker_symbol) REFERENCES company_info(ticker_symbol),
    UNIQUE KEY uk_portfolio_ticker (portfolio_id, ticker_symbol)
);

-- 表8：PriceAlert 个股止盈止损条件 触发警告条件
CREATE TABLE price_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id VARCHAR(50) NOT NULL,
    ticker_symbol VARCHAR(50) NOT NULL,
    target_price DECIMAL(15, 4) NOT NULL,
    alert_type VARCHAR(20) NOT NULL, -- 'STOP_LOSS' 止损, 'TAKE_PROFIT' 止盈
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(portfolio_id),
    FOREIGN KEY (ticker_symbol) REFERENCES company_info(ticker_symbol)
);