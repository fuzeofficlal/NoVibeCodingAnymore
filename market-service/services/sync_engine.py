import time
import random
import yfinance as yf
from datetime import datetime, timedelta
import pandas as pd
import requests
import io
import os
import json
from sqlalchemy import func
from sqlalchemy.dialects.mysql import insert
from models import CompanyInfo, HistoricalPrice

def get_sp500_tickers():
    cache_file = 'sp500_cache.json'
    if os.path.exists(cache_file):
        with open(cache_file, 'r', encoding='utf-8') as f:
            return json.load(f)

    url = 'https://en.wikipedia.org/wiki/List_of_S%26P_500_companies'
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)'
    }
    response = requests.get(url, headers=headers)
    response.raise_for_status()

    tables = pd.read_html(io.StringIO(response.text))
    df = tables[0]
    tickers = df['Symbol'].str.replace('.', '-').tolist()
    names = df['Security'].tolist()

    stock_list = list(zip(tickers, names))

    with open(cache_file, 'w', encoding='utf-8') as f:
        json.dump(stock_list, f, ensure_ascii=False, indent=4)

    return stock_list

def catch_up_sync(db):
    print("[SYNC]ing")

    # 1. Fetch SP500 & Extra Tickers
    sp500_raw = get_sp500_tickers()
    sp500_stocks = [(t, n, 'STOCK') for t, n in sp500_raw]

    EXTRA_TICKERS = [
        # Cryptocurrencies (its is7x24)
        ('BTC-USD', 'Bitcoin', 'CRYPTO'),
        ('ETH-USD', 'Ethereum', 'CRYPTO'),
        ('USDT-USD', 'Tether', 'CRYPTO'),
        ('BNB-USD', 'BNB', 'CRYPTO'),
        ('SOL-USD', 'Solana', 'CRYPTO'),
        ('XRP-USD', 'XRP', 'CRYPTO'),
        ('DOGE-USD', 'Dogecoin', 'CRYPTO'),
        ('ADA-USD', 'Cardano', 'CRYPTO'),
        ('USDC-USD', 'USD Coin', 'CRYPTO'),
        
        # US Treasury Yields
        ('^TNX', 'Treasury Yield 10 Years', 'BOND'),
        ('^FVX', 'Treasury Yield 5 Years', 'BOND'),
        ('^TYX', 'Treasury Yield 30 Years', 'BOND'),
        
        # Bond ETFs
        ('TLT', 'iShares 20+ Year Treasury Bond ETF', 'BOND'),
        ('AGG', 'iShares Core US Aggregate Bond ETF', 'BOND'),
        ('BND', 'Vanguard Total Bond Market ETF', 'BOND'),
        ('LQD', 'iShares iBoxx $ Investment Grade Corporate Bond ETF', 'BOND'),
        ('HYG', 'iShares iBoxx $ High Yield Corporate Bond ETF', 'BOND'),
        
        # China A
        ('600519.SS', 'Kweichow Moutai', 'STOCK'),
        ('300750.SZ', 'CATL', 'STOCK'),
        ('000858.SZ', 'Wuliangye Yibin', 'STOCK'),
        ('601318.SS', 'Ping An Insurance', 'STOCK'),
        ('600036.SS', 'China Merchants Bank', 'STOCK'),
        ('601012.SS', 'LONGi Green Energy', 'STOCK'),
        ('002594.SZ', 'BYD Co.', 'STOCK'),
        ('600276.SS', 'Jiangsu Hengrui Medicine', 'STOCK'),
        ('000333.SZ', 'Midea Group', 'STOCK'),
        ('601888.SS', 'China Tourism Group Duty Free', 'STOCK'),
        ('603288.SS', 'Foshan Haitian Flavouring', 'STOCK'),
        ('600030.SS', 'CITIC Securities', 'STOCK'),
        ('600900.SS', 'China Yangtze Power', 'STOCK'),
        ('601166.SS', 'Industrial Bank Co.', 'STOCK'),
        ('300059.SZ', 'East Money Information', 'STOCK'),
        ('002415.SZ', 'Hikvision', 'STOCK'),
        ('600028.SS', 'Sinopec', 'STOCK'),
        ('601088.SS', 'China Shenhua Energy', 'STOCK'),
        ('000568.SZ', 'Luzhou Laojiao', 'STOCK'),
        ('601998.SS', 'China CITIC Bank', 'STOCK'),
        ('002714.SZ', 'Muyuan Foods', 'STOCK'),
        ('601398.SS', 'ICBC', 'STOCK'),
        ('601288.SS', 'Agricultural Bank of China', 'STOCK'),
        ('601939.SS', 'China Construction Bank', 'STOCK'),
        ('601988.SS', 'Bank of China', 'STOCK'),
        ('600887.SS', 'Yili Group', 'STOCK'),
        ('603259.SS', 'WuXi AppTec', 'STOCK'),
        ('300015.SZ', 'Aier Eye Hospital', 'STOCK'),
        ('300122.SZ', 'Chongqing Zhifei Biological', 'STOCK'),
        ('300014.SZ', 'EVE Energy', 'STOCK'),
        ('600309.SS', 'Wanhua Chemical', 'STOCK'),
        ('000002.SZ', 'China Vanke Co.', 'STOCK'),
        ('000001.SZ', 'Ping An Bank', 'STOCK'),
        ('000166.SZ', 'Shenwan Hongyuan', 'STOCK'),
        ('601628.SS', 'China Life Insurance', 'STOCK'),
        ('601857.SS', 'PetroChina', 'STOCK'),
        ('601668.SS', 'China State Construction', 'STOCK'),
        ('601328.SS', 'Bank of Communications', 'STOCK'),
        ('600016.SS', 'Minsheng Bank', 'STOCK'),
        ('600000.SS', 'Shanghai Pudong Development Bank', 'STOCK'),
        ('600048.SS', 'Poly Developments', 'STOCK'),
        ('601818.SS', 'China Everbright Bank', 'STOCK'),
        ('600104.SS', 'SAIC Motor', 'STOCK'),
        ('601186.SS', 'China Railway Construction', 'STOCK'),
        ('601766.SS', 'CRRC', 'STOCK'),
        ('601006.SS', 'Daqin Railway', 'STOCK'),
        ('601989.SS', 'China Shipbuilding Industry', 'STOCK'),
        ('601800.SS', 'China Communications Construction', 'STOCK'),
        ('601111.SS', 'Air China', 'STOCK'),
        ('600019.SS', 'Baoshan Iron & Steel', 'STOCK')
    ]
    all_stocks = sp500_stocks + EXTRA_TICKERS
    
    total_tickers = len(all_stocks)
    print(f"[SYNC] Found {total_tickers} assets to process.")

    for idx, (ticker, name, a_type) in enumerate(all_stocks, start=1):
        try:
            stmt_comp = insert(CompanyInfo).values([{"ticker_symbol": ticker, "company_name": name, "asset_type": a_type}])
            stmt_comp = stmt_comp.on_duplicate_key_update(company_name=name, asset_type=a_type)
            db.execute(stmt_comp)
            watermark = db.query(func.max(HistoricalPrice.trade_date))\
                          .filter(HistoricalPrice.ticker_symbol == ticker)\
                          .scalar()

            start_date_str = "2021-01-01"
            if watermark:
                start_date_str = watermark.strftime('%Y-%m-%d')
                today_str = datetime.now().strftime('%Y-%m-%d')
                if start_date_str >= today_str:
                    print(f"[{idx}/{total_tickers}] ⚡ Skipped: {ticker} ({name}) - Up to date ({watermark.strftime('%Y-%m-%d')})")
                    continue

            stock = yf.Ticker(ticker)
            hist = stock.history(start=start_date_str)

            if hist.empty:
                print(f"[WARN] [{idx}/{total_tickers}] {ticker} returned no data.")
                time.sleep(random.uniform(0.5, 1.5))
                continue
            if hist.index.tz is not None:
                hist.index = hist.index.tz_localize(None)

            records = []
            for dt, row in hist.iterrows():
                trade_date_val = dt
                close_price_val = float(row['Close'])
                records.append({
                    "ticker_symbol": ticker,
                    "trade_date": trade_date_val,
                    "close_price": close_price_val
                })

            if not records:
                continue

            stmt = insert(HistoricalPrice).values(records)
            upsert_stmt = stmt.on_duplicate_key_update(
                close_price=stmt.inserted.close_price
            )
            
            db.execute(upsert_stmt)
            db.commit()

            print(f"[OK] [{idx}/{total_tickers}] {ticker}: Synced {len(records)} records (Watermark ref: {start_date_str})")
            
            time.sleep(random.uniform(0.5, 1.5))

        except Exception as e:
            db.rollback()
            err_str = str(e)
            if "Too Many Requests" in err_str or "Rate limit" in err_str:
                print(f"[RATE LIMIT HIT] Sleeping for 45s... ({ticker})")
                time.sleep(45)
            else:
                print(f"[ERROR] syncing {ticker}: {e}")
                time.sleep(random.uniform(0.5, 1.5))
        
    print("[SYNC] Catch-up Sync Complete!")
