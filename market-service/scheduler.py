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
    
    # 判断传统美股是否开盘
    is_market_open = True
    if now_nyc.weekday() >= 5: # 周末
        is_market_open = False
    elif now_nyc.hour < 9 or (now_nyc.hour == 9 and now_nyc.minute < 30):
        is_market_open = False
    elif now_nyc.hour >= 16:
        is_market_open = False
        
    print(f"[SCHEDULER] Triggering Real-Time Polling (US Market Open: {is_market_open})")
    db = SessionLocal()
    try:
        # 如果开盘，传 crypto_only=False，抓取所有资产的快照
        # 如果停盘，传 crypto_only=True，仅抓取 7x24 不停盘的数字货币
        poll_realtime_prices(db, crypto_only=(not is_market_open))
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
    
    # 取消 9-16 和 工作日的限制，开启 7x24 小时无脑轮回。拦截器下放到 run_polling_task
    scheduler.add_job(
        run_polling_task,
        "cron",
        minute='*/5',  # 每 5 分钟执行一次，不受日期限制
        timezone='America/New_York',
        id="realtime_polling",
        replace_existing=True
    )

    scheduler.start()
    print("[SCHEDULER] APScheduler started. Tracking America/New_York timezone.")
    return scheduler
