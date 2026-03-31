import asyncio
import json
from typing import List
from fastapi import WebSocket
from database import SessionLocal
from models import MarketPrice
import random
import time

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []
        self.base_prices = {}
        self.last_db_poll = 0

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    def _sync_fetch_base_prices(self):
        db = SessionLocal()
        try:
            records = db.query(MarketPrice).all()
            for r in records:
                self.base_prices[r.ticker_symbol] = r.current_price
            self.last_db_poll = time.time()
        finally:
            db.close()

    async def broadcast_demo_ticks(self):

        while True:

            if time.time() - self.last_db_poll > 60:
                self._sync_fetch_base_prices()

            if self.active_connections and self.base_prices:
                payload = {"timestamp": int(time.time() * 1000), "prices": {}}

                for ticker, price in self.base_prices.items():
                    if not price:
                        price = 100.0  # Fallback
                    payload["prices"][ticker] = float(price)
                
                #bbroadcast payload
                json_payload = json.dumps(payload)
                
                # Send all
                disconnected = []
                for connection in self.active_connections:
                    try:
                        await connection.send_text(json_payload)
                    except Exception:
                        disconnected.append(connection)
                
                for dead_conn in disconnected:
                    self.disconnect(dead_conn)

            # Wait exactly 1 second for the next tick
            await asyncio.sleep(1)

manager = ConnectionManager()
