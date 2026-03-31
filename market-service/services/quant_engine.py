from typing import List, Dict
import pandas as pd
from sqlalchemy import select
from database import engine
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from models import HistoricalPrice

def calculate_sma(ticker: str, days: int) -> List[Dict]:
   
    with engine.connect() as conn:
        query = select(HistoricalPrice.trade_date, HistoricalPrice.close_price)\
            .where(HistoricalPrice.ticker_symbol == ticker)\
            .order_by(HistoricalPrice.trade_date.desc())\
            .limit(days + 200)
            
        df = pd.read_sql(query, conn)

    if df.empty:
        return []

    df = df.sort_values('trade_date').reset_index(drop=True)

    df['sma'] = df['close_price'].astype(float).rolling(window=days).mean()

    df = df.dropna(subset=['sma'])
    
    df = df.tail(100)

    return [
        {
            "date": row['trade_date'].strftime('%Y-%m-%d'),
            "sma": round(float(row['sma']), 4)
        }
        for _, row in df.iterrows()
    ]
