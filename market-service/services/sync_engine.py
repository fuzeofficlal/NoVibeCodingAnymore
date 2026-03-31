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
    sp500_stocks = get_sp500_tickers()
    EXTRA_TICKERS = [
        # Cryptocurrencies (its is7x24)
        ('BTC-USD', 'Bitcoin'),
        ('ETH-USD', 'Ethereum'),
        ('USDT-USD', 'Tether'),
        ('BNB-USD', 'BNB'),
        ('SOL-USD', 'Solana'),
        ('XRP-USD', 'XRP'),
        ('DOGE-USD', 'Dogecoin'),
        ('ADA-USD', 'Cardano'),
        ('USDC-USD', 'USD Coin'),
        
        # US Treasury Yields
        ('^TNX', 'Treasury Yield 10 Years'),
        ('^FVX', 'Treasury Yield 5 Years'),
        ('^TYX', 'Treasury Yield 30 Years'),
        
        # Bond ETFs
        ('TLT', 'iShares 20+ Year Treasury Bond ETF'),
        ('AGG', 'iShares Core US Aggregate Bond ETF'),
        ('BND', 'Vanguard Total Bond Market ETF'),
        ('LQD', 'iShares iBoxx $ Investment Grade Corporate Bond ETF'),
        ('HYG', 'iShares iBoxx $ High Yield Corporate Bond ETF')
    ]
    all_stocks = sp500_stocks + EXTRA_TICKERS
    
    total_tickers = len(all_stocks)
    print(f"[SYNC] Found {total_tickers} assets to process.")

    for idx, (ticker, name) in enumerate(all_stocks, start=1):
        try:
            stmt_comp = insert(CompanyInfo).values([{"ticker_symbol": ticker, "company_name": name}])
            stmt_comp = stmt_comp.on_duplicate_key_update(company_name=name)
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
