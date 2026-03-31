import time
import yfinance as yf
from datetime import datetime
import pandas as pd
from sqlalchemy.dialects.mysql import insert
from models import CompanyInfo, MarketPrice

def poll_realtime_prices(db, crypto_only=False):

    mode_str = "CRYPTO-ONLY" if crypto_only else "ALL-ASSETS"
    print(f"[POLL] Triggered Real-time Snapshot ({mode_str}) at {datetime.now()} EST")

    companies = db.query(CompanyInfo.ticker_symbol).all()
    if not companies:
        print("[WARN] newport_db has no company info!")
        return

    tickers = [c[0] for c in companies]
    
    if crypto_only:
        tickers = [t for t in tickers if '-USD' in t.upper() or t.upper() in ['BTC', 'ETH']]
        if not tickers:
            return # 压根没有数字货币，直接跳过!
            
    tickers_string = " ".join(tickers)


    records = []
    missing_tickers = tickers.copy()
    max_retries = 3

    for attempt in range(max_retries):
        if not missing_tickers:
            break # 完美抓齐所有数据！提前收工

        tickers_string = " ".join(missing_tickers)
        try:
            print(f"[FETCH] Real-time 1d snapshot {len(missing_tickers)} tickers (Attempt {attempt+1}/{max_retries})...")
            # 杀手锏：关闭 threads 并发，极大降低雅虎财经返回 JSON 'NoneType' 的解析报错概率
            snapshot = yf.download(tickers_string, period="1d", threads=False, progress=False)

            if snapshot.empty:
                print(f"[WARN] empty body on attempt {attempt+1}")
                time.sleep(2 ** attempt)
                continue

            found_this_round = []
            
            if len(missing_tickers) == 1:
                ticker = missing_tickers[0]
                if 'Close' in snapshot.columns:
                    close_price = snapshot['Close'].iloc[-1]
                    if not pd.isna(close_price):
                        records.append({
                            "ticker_symbol": ticker,
                            "current_price": float(close_price)
                        })
                        found_this_round.append(ticker)
            else:
                if 'Close' in snapshot.columns.levels[0]:
                    close_df = snapshot['Close']
                    for ticker in missing_tickers:
                        if ticker in close_df.columns:
                            latest_price = close_df[ticker].iloc[-1]
                            if not pd.isna(latest_price):
                                records.append({
                                    "ticker_symbol": ticker,
                                    "current_price": float(latest_price)
                                })
                                found_this_round.append(ticker)
            
            # 成功落袋为安的数据，从 missing_tickers 里剔除，下次重试只抓没成功的！
            for t in found_this_round:
                missing_tickers.remove(t)

            if missing_tickers:
                print(f"[WARN] Missing {len(missing_tickers)} tickers {missing_tickers}. Backing off to retry...")
                time.sleep(2 ** attempt)

        except Exception as e:
            err_str = str(e)
            if "Too Many" in err_str or "Rate limit" in err_str:
                print("[RATE LIMIT HIT] 429 restriction. Backing off 5s...")
                time.sleep(5)
            else:
                print(f"[ERROR] batched yf.download: {e}")
                time.sleep(2)

    if not records:
        print("[ERROR] F**k, completely failed to fetch any prices after 3 attempts")
        return

    try:
        stmt = insert(MarketPrice).values(records)
        upsert_stmt = stmt.on_duplicate_key_update(
            current_price=stmt.inserted.current_price
        )

        db.execute(upsert_stmt)
        db.commit()

        print(f"[OK] [Polling Engine] Successfully flushed {len(records)} updated market prices to DB.")

    except Exception as e:
        db.rollback()
        print(f"[DB ERROR] failed to UPSERT market_price: {e}")
