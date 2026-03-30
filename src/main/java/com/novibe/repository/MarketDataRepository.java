package com.novibe.repository;

import com.novibe.entity.MarketData;
import com.novibe.entity.MarketDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, MarketDataId> {
    // GET /api/assets/{ticker}/price 暂定 获取某个资产的最新价格
    Optional<MarketData> findTopByTickerSymbolOrderByTimestampDesc(String tickerSymbol);
}
