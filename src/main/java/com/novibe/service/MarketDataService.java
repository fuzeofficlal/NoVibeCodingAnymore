package com.novibe.service;

import com.novibe.entity.CompanyInfo;
import com.novibe.entity.MarketData;
import com.novibe.exception.ResourceNotFoundException;
import com.novibe.repository.CompanyInfoRepository;
import com.novibe.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final CompanyInfoRepository companyInfoRepository;
    private final MarketDataRepository marketDataRepository;


     //  list all assets GET /api/assets
    public List<CompanyInfo> getAvailableAssets() {
        return companyInfoRepository.findAll();
    }


     // list specific for one ticker GET /api/assets/{ticker}/price

    public MarketData getLatestPrice(String tickerSymbol) {
        return marketDataRepository.findTopByTickerSymbolOrderByTimestampDesc(tickerSymbol)
                .orElseThrow(() -> new ResourceNotFoundException("No market data found for ticker: " + tickerSymbol));
    }

    public List<MarketData> getHistoricalData(String tickerSymbol) {
        return marketDataRepository.findByTickerSymbolOrderByTimestampAsc(tickerSymbol);
    }
}
