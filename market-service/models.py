from sqlalchemy import Column, String, Date, Numeric, TIMESTAMP, text, DATETIME
from database import Base

class CompanyInfo(Base):
    __tablename__ = "company_info"
    
    ticker_symbol = Column(String(50), primary_key=True, index=True)
    company_name = Column(String(255))
    asset_type = Column(String(20), default='STOCK')

class HistoricalPrice(Base):
    __tablename__ = "market_data"
    
    ticker_symbol = Column(String(50), primary_key=True)
    trade_date = Column("timestamp", DATETIME, primary_key=True)
    close_price = Column(Numeric(15, 4))

class MarketPrice(Base):
    __tablename__ = "market_price"
    
    ticker_symbol = Column(String(20), primary_key=True, index=True)
    current_price = Column(Numeric(15, 4))
    last_updated = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))
