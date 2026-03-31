import pymysql
import yfinance as yf
import pandas as pd
from datetime import datetime, timedelta
import time
import requests
import io
import random
import os
import json


DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3306,
    'user': 'root',
    'password': 'Azhe114514',  
    'database': 'newport_db', 
    'charset': 'utf8mb4',
    'autocommit': True
}

def init_db():
    temp_conn = pymysql.connect(
        host=DB_CONFIG['host'], port=DB_CONFIG['port'],
        user=DB_CONFIG['user'], password=DB_CONFIG['password']
    )
    with temp_conn.cursor() as cursor:
        cursor.execute(f"CREATE DATABASE IF NOT EXISTS {DB_CONFIG['database']} DEFAULT CHARSET utf8mb4")
    temp_conn.close()

    conn = pymysql.connect(**DB_CONFIG)
    with conn.cursor() as cursor:

        cursor.execute('''
                       CREATE TABLE IF NOT EXISTS company_info
                       (
                           ticker_symbol VARCHAR(50) PRIMARY KEY,
                           company_name VARCHAR(255)
                       )
                       ''')
        cursor.execute('''
                       CREATE TABLE IF NOT EXISTS market_data
                       (
                           ticker_symbol VARCHAR(50),
                           timestamp DATETIME,
                           close_price DECIMAL(15, 4),
                           PRIMARY KEY (ticker_symbol, timestamp)
                       )
                       ''')
    return conn


def get_sp500_tickers():
    cache_file = 'sp500_cache.json'

    if os.path.exists(cache_file):
        print("📦 發現本地緩存！正在從 sp500_cache.json 讀取股票列表...")
        with open(cache_file, 'r', encoding='utf-8') as f:
            return json.load(f)

    print("🌐 緩存不存在，正在從維基百科獲取 S&P 500 股票列表...")
    url = 'https://en.wikipedia.org/wiki/List_of_S%26P_500_companies'
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    response = requests.get(url, headers=headers)
    response.raise_for_status()

    tables = pd.read_html(io.StringIO(response.text))
    df = tables[0]
    tickers = df['Symbol'].str.replace('.', '-').tolist()
    names = df['Security'].tolist()

    stock_list = list(zip(tickers, names))

    print("💾 正在將股票名單保存到本地緩存 (sp500_cache.json)...")
    with open(cache_file, 'w', encoding='utf-8') as f:
        json.dump(stock_list, f, ensure_ascii=False, indent=4)

    return stock_list


def fetch_and_save_historical_data(conn, stock_list):
    total_stocks = len(stock_list)
    print(f"\n准备抓取 {total_stocks} 只股票从 2021-01-01 至今的历史数据...")

    with conn.cursor() as cursor:
        for index, (ticker, name) in enumerate(stock_list, 1):
            max_retries = 3  # 最大重试次数

            for attempt in range(max_retries):
                try:

                    cursor.execute('''
                                   INSERT INTO company_info (ticker_symbol, company_name)
                                   VALUES (%s, %s) ON DUPLICATE KEY
                                   UPDATE company_name=VALUES(company_name)
                                   ''', (ticker, name))

                    # --- 增量更新核心逻辑 ---
                    cursor.execute("SELECT MAX(timestamp) FROM market_data WHERE ticker_symbol = %s", (ticker,))
                    result = cursor.fetchone()
                    max_date = result[0] if result and result[0] else None

                    start_date_str = "2021-01-01"
                    if max_date is not None:
                        # 修复与纽约时差导致的 startDate > endDate 报错：
                        # 直接用 max_date 覆盖请求，ON DUPLICATE KEY UPDATE 会自动更新重叠的一天，
                        # 可以顺便修复之前抓取时的半截残缺数据（盘中数据被收盘数据覆盖）
                        start_date_str = max_date.strftime('%Y-%m-%d')
                        
                        # 如果数据库里的时间已经达到或超过了您这边的「今天」，就无需再去求雅虎了，直接跳过
                        today_str = datetime.now().strftime('%Y-%m-%d')
                        if start_date_str >= today_str:
                            print(f"[{index}/{total_stocks}] ⚡ 跳过: {ticker} ({name}) - 已经是最新数据截至 {max_date.strftime('%Y-%m-%d')}")
                            break # 这只股票已是最新，跳出重试循环进入下一只

                    stock = yf.Ticker(ticker)
                    hist = stock.history(start=start_date_str)

                    if not hist.empty:
                        records = []
                        for date, row in hist.iterrows():
                            # Java LocalDateTime 适配格式
                            trade_date_str = date.strftime('%Y-%m-%d 00:00:00')
                            close_price = float(row['Close'])
                            records.append((ticker, trade_date_str, close_price))

                        sql = '''
                              INSERT INTO market_data (ticker_symbol, timestamp, close_price)
                              VALUES (%s, %s, %s) ON DUPLICATE KEY 
                              UPDATE close_price = VALUES(close_price)
                              '''
                        cursor.executemany(sql, records)

                        if index % 50 == 0:
                            conn.commit()

                        print(f"[{index}/{total_stocks}] 成功: {ticker} ({name}) - 写入 {len(records)} 条")
                    else:
                        print(f"[{index}/{total_stocks}] 警告: {ticker} 没有返回历史数据")


                    break

                except Exception as e:
                    error_msg = str(e)
                    # 拦截 429 频率限制错误
                    if "Too Many Requests" in error_msg or "Rate limited" in error_msg:
                        if attempt < max_retries - 1:
                            # 指数退避休眠
                            sleep_time = 10 * (attempt + 1) + random.uniform(1.0, 3.0)
                            print(f"  ⚠️ 触发限流... 休眠 {sleep_time:.1f} 秒后进行第 {attempt + 1} 次重试")
                            time.sleep(sleep_time)
                        else:
                            print(f"[{index}/{total_stocks}] ❌ 彻底失败: {ticker} 被拦截。")
                    else:
                        print(f"[{index}/{total_stocks}] 错误: 抓取 {ticker} 失败。原因: {e}")
                        break  # 如果是其他未知错误，直接跳过这只股票

            # 正常情况下的基础休眠
            time.sleep(random.uniform(1.0, 3.0))
            conn.commit()

    print("\n✅ 所有历史数据写入流程结束！")

if __name__ == "__main__":
    db_connection = init_db()
    sp500_stocks = get_sp500_tickers()

    EXTRA_TICKERS = [
        ('BTC-USD', 'Bitcoin'),
        ('ETH-USD', 'Ethereum'),
        ('USDT-USD', 'Tether'),
        ('TLT', 'iShares 20+ Year Treasury Bond ETF'),
        ('AGG', 'iShares Core US Aggregate Bond ETF'),
        ('^TNX', 'Treasury Yield 10 Years')
    ]
    
    all_stocks = sp500_stocks + EXTRA_TICKERS

    fetch_and_save_historical_data(db_connection, all_stocks)

    db_connection.close()