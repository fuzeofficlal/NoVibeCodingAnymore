from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
import pytz
from datetime import datetime

from database import SessionLocal
from services.sync_engine import catch_up_sync
from services.polling_engine import poll_realtime_prices

def run_sync_task():
 
    print("[SCHEDULER] Executing Scheduled Catch-Up Sync (EOD)...")
    db = SessionLocal()
    try:
        catch_up_sync(db)
    finally:
        db.close()

def run_polling_task():
    nyc_tz = pytz.timezone('America/New_York')
    now_nyc = datetime.now(nyc_tz)
    
    # 1. 判断美股是否开盘
    is_us_open = True
    if now_nyc.weekday() >= 5: # 周末
        is_us_open = False
    elif now_nyc.hour < 9 or (now_nyc.hour == 9 and now_nyc.minute < 30):
        is_us_open = False
    elif now_nyc.hour >= 16:
        is_us_open = False

    # 2. 判断 A 股是否开盘 (北京时间 09:30 - 15:00, Mon-Fri)
    bj_tz = pytz.timezone('Asia/Shanghai')
    now_bj = datetime.now(bj_tz)
    
    is_cn_open = True
    if now_bj.weekday() >= 5: # 周末
        is_cn_open = False
    elif now_bj.hour < 9 or (now_bj.hour == 9 and now_bj.minute < 30):
        is_cn_open = False
    elif now_bj.hour >= 15:
        is_cn_open = False
        
    any_market_open = is_us_open or is_cn_open
    
    print(f"[SCHEDULER] Real-Time Polling (US Open: {is_us_open} | CN Open: {is_cn_open})")
    db = SessionLocal()
    try:


        # 如果中美至少有一个开盘，crypto_only=False抓取所有资产
        poll_realtime_prices(db, crypto_only=(not any_market_open))
    finally:
        db.close()


def start_scheduler():
    scheduler = BackgroundScheduler(timezone=pytz.timezone('America/New_York'))
    
    scheduler.add_job(
        run_sync_task,
        CronTrigger(day_of_week='mon-fri', hour=18, minute=0, timezone='America/New_York'),
        id="catch_up_eod",
        replace_existing=True
    )
    

    scheduler.add_job(
        run_polling_task,
        "cron",
        minute='*/5',
        timezone='America/New_York',
        id="realtime_polling",
        replace_existing=True
    )

    scheduler.start()
    print("[SCHEDULER] APScheduler started. Tracking America/New_York timezone.")
    return scheduler
