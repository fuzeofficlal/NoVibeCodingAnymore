from database import SessionLocal
from services.polling_engine import poll_realtime_prices

print("--- FORCED POLLING START ---")
db = SessionLocal()
try:
    poll_realtime_prices(db, crypto_only=False)
finally:
    db.close()
print("--- FORCED POLLING DONE ---")
