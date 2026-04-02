USE newport_db;

DELETE FROM position WHERE portfolio_id='demo_master';
DELETE FROM portfolio_transaction WHERE portfolio_id='demo_master';
DELETE FROM portfolio WHERE portfolio_id='demo_master';
DELETE FROM watchlist WHERE portfolio_id='demo_master';

INSERT INTO portfolio (portfolio_id, name, cash_balance) VALUES ('demo_master', 'Quantum Alpha Demo', 47500.00);

INSERT INTO portfolio_transaction (transaction_id, portfolio_id, transaction_type, quantity, price_per_unit, transaction_date)
VALUES ('demo_tx_dep01', 'demo_master', 'DEPOSIT', 500000.00, 1.00, DATE_SUB(NOW(), INTERVAL 60 DAY));

INSERT INTO portfolio_transaction (transaction_id, portfolio_id, ticker_symbol, transaction_type, quantity, price_per_unit, transaction_date)
VALUES ('demo_tx_aapl_buy', 'demo_master', 'AAPL', 'BUY', 500.00, 150.00, DATE_SUB(NOW(), INTERVAL 45 DAY));
INSERT INTO position (position_id, portfolio_id, ticker_symbol, total_quantity, average_cost)
VALUES ('demo_pos_aapl', 'demo_master', 'AAPL', 500.00, 150.00);

INSERT INTO portfolio_transaction (transaction_id, portfolio_id, ticker_symbol, transaction_type, quantity, price_per_unit, transaction_date)
VALUES ('demo_tx_nvda_buy', 'demo_master', 'NVDA', 'BUY', 1000.00, 80.00, DATE_SUB(NOW(), INTERVAL 50 DAY));
INSERT INTO position (position_id, portfolio_id, ticker_symbol, total_quantity, average_cost)
VALUES ('demo_pos_nvda', 'demo_master', 'NVDA', 1000.00, 80.00);

INSERT INTO portfolio_transaction (transaction_id, portfolio_id, ticker_symbol, transaction_type, quantity, price_per_unit, transaction_date)
VALUES ('demo_tx_btc_buy', 'demo_master', 'BTC-USD', 'BUY', 2.00, 45000.00, DATE_SUB(NOW(), INTERVAL 30 DAY));
INSERT INTO position (position_id, portfolio_id, ticker_symbol, total_quantity, average_cost)
VALUES ('demo_pos_btc', 'demo_master', 'BTC-USD', 2.00, 45000.00);

INSERT INTO portfolio_transaction (transaction_id, portfolio_id, ticker_symbol, transaction_type, quantity, price_per_unit, transaction_date)
VALUES ('demo_tx_tlt_buy', 'demo_master', 'TLT', 'BUY', 500.00, 95.00, DATE_SUB(NOW(), INTERVAL 15 DAY));
INSERT INTO position (position_id, portfolio_id, ticker_symbol, total_quantity, average_cost)
VALUES ('demo_pos_tlt', 'demo_master', 'TLT', 500.00, 95.00);

INSERT INTO portfolio_transaction (transaction_id, portfolio_id, ticker_symbol, transaction_type, quantity, price_per_unit, transaction_date)
VALUES ('demo_tx_tsla_buy', 'demo_master', 'TSLA', 'BUY', 200.00, 200.00, DATE_SUB(NOW(), INTERVAL 40 DAY));
INSERT INTO position (position_id, portfolio_id, ticker_symbol, total_quantity, average_cost)
VALUES ('demo_pos_tsla', 'demo_master', 'TSLA', 200.00, 200.00);

INSERT INTO watchlist (watchlist_id, portfolio_id, ticker_symbol) VALUES ('demo_wtch_01', 'demo_master', '000001.SZ');
