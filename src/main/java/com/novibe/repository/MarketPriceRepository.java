package com.novibe.repository;

import com.novibe.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, String> {
    
    // 批量查询多个资产的实时价格
    List<MarketPrice> findByTickerSymbolIn(List<String> tickers);
}
