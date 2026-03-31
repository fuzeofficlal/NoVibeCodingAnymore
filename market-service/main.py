from fastapi import FastAPI, Depends, HTTPException, Query, BackgroundTasks, WebSocket
from sqlalchemy.orm import Session
from typing import List
import asyncio

from database import engine, get_db
import models
from scheduler import start_scheduler, run_sync_task
from services import quant_engine

app = FastAPI(
    title="Market Data Microservice",
    description=" historical syncs and real-time polling from Yahoo Finance",
    version="1.0.0"
)

# Application Lifecycle Hook
@app.on_event("startup")
async def startup_event():
    print("[STARTUP] [FastAPI] Initializing Market Data Service...")
    
    try:
        from sqlalchemy import text
        with engine.begin() as conn:
            conn.execute(text('''
                CREATE TABLE IF NOT EXISTS company_info (
                    ticker_symbol VARCHAR(50) PRIMARY KEY,
                    company_name VARCHAR(255)
                )
            '''))
            conn.execute(text('''
                CREATE TABLE IF NOT EXISTS market_data (
                    ticker_symbol VARCHAR(50),
                    timestamp DATETIME,
                    close_price DECIMAL(15, 4),
                    PRIMARY KEY (ticker_symbol, timestamp)
                )
            '''))
            conn.execute(text('''
                CREATE TABLE IF NOT EXISTS market_price (
                    ticker_symbol VARCHAR(50) PRIMARY KEY,
                    current_price DECIMAL(15, 4),
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            '''))
            print("[STARTUP] Verified tables schemas successfully.")
    except Exception as e:
        print(f"[STARTUP ERROR] DB Initialization failed: {e}")

    # APScheduler
    start_scheduler()

    from websocket_manager import manager
    asyncio.create_task(manager.broadcast_demo_ticks())
    
    # catch-up sync
   
    print("[STARTUP] [FastAPI]waiting for user input sync ins.")
from services.polling_engine import poll_realtime_prices
from database import SessionLocal

def execute_full_override_sync():

    #Historical
    run_sync_task()
    
    print("[MANUAL]Forced Real-Time Polling")
    db = SessionLocal()
    try:
        poll_realtime_prices(db)
    finally:
        db.close()

# Trigger sync at once
@app.post("/api/v1/market/sync")
async def manual_sync(background_tasks: BackgroundTasks):
    background_tasks.add_task(execute_full_override_sync)
    return {"status": "accepted", "message": "Background ETL Sync Started."}

# Get all current real-time market prices
@app.get("/api/v1/market/prices")
async def get_market_prices(
    tickers: str = Query(None, description="Comma-separated ticker symbols (e.g. AAPL,MSFT)"),
    db: Session = Depends(get_db)
):
    query = db.query(models.MarketPrice)
    if tickers:
        ticker_list = [t.strip().upper() for t in tickers.split(',')]
        query = query.filter(models.MarketPrice.ticker_symbol.in_(ticker_list))
    
    results = query.all()
    return [{"ticker_symbol": r.ticker_symbol, "current_price": r.current_price, "last_updated": r.last_updated} for r in results]

# historical close prices
@app.get("/api/v1/market/history/{ticker}")
async def get_historical_prices(
    ticker: str,
    start_date: str = Query(None, description="Start date (YYYY-MM-DD)"),
    end_date: str = Query(None, description="End date (YYYY-MM-DD)"),
    db: Session = Depends(get_db)
):
    query = db.query(models.HistoricalPrice)\
              .filter(models.HistoricalPrice.ticker_symbol == ticker.upper())\
              .order_by(models.HistoricalPrice.trade_date.asc())
              
    if start_date:
        query = query.filter(models.HistoricalPrice.trade_date >= start_date)
    if end_date:
        query = query.filter(models.HistoricalPrice.trade_date <= end_date)
        
    results = query.all()
    return [{"trade_date": r.trade_date, "close_price": r.close_price} for r in results]
# try SMA here
@app.get("/api/v1/market/indicators/sma/{ticker}")
async def get_sma_indicator(ticker: str, days: int = Query(50, description="Rolling window days (e.g. 20, 50, 200)")):
    return quant_engine.calculate_sma(ticker, days)

# WebSocket Real-Time Stream Endpoint
@app.websocket("/api/v1/market/ws")
async def websocket_endpoint(websocket: WebSocket):
    from websocket_manager import manager
    await manager.connect(websocket)
    try:
        while True:
            # keep alive
            await websocket.receive_text()
    except Exception:
        manager.disconnect(websocket)
