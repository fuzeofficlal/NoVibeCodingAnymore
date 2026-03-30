package com.novibe.service;

import com.novibe.dto.AssetAllocationDTO;
import com.novibe.dto.HistoricalPerformanceDTO;
import com.novibe.dto.PortfolioOverviewDTO;
import com.novibe.dto.PositionDetailDTO;
import com.novibe.entity.MarketData;
import com.novibe.entity.Portfolio;
import com.novibe.entity.Position;
import com.novibe.exception.ResourceNotFoundException;
import com.novibe.repository.MarketDataRepository;
import com.novibe.repository.PortfolioRepository;
import com.novibe.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalyticsService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final MarketDataService marketDataService;
    private final MarketDataRepository marketDataRepository;

    public PortfolioOverviewDTO getPortfolioOverview(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for: " + portfolioId));
        
        List<Position> positions = positionRepository.findByPortfolioPortfolioId(portfolioId);

        BigDecimal cashBalance = portfolio.getCashBalance();
        BigDecimal holdingsValue = BigDecimal.ZERO;
        BigDecimal holdingsCost = BigDecimal.ZERO;

        for (Position p : positions) {
            BigDecimal currentPrice = marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol()).getClosePrice();
            holdingsValue = holdingsValue.add(p.getTotalQuantity().multiply(currentPrice));
            holdingsCost = holdingsCost.add(p.getTotalQuantity().multiply(p.getAverageCost()));
        }

        BigDecimal totalValue = cashBalance.add(holdingsValue);
        BigDecimal totalCost = cashBalance.add(holdingsCost);

        BigDecimal totalReturnPercentage = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnPercentage = totalValue.subtract(totalCost)
                    .divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return PortfolioOverviewDTO.builder()
                .cashBalance(cashBalance)
                .totalValue(totalValue)
                .totalCost(totalCost)
                .totalReturnPercentage(totalReturnPercentage)
                .build();
    }

    public List<AssetAllocationDTO> getAssetAllocationSummary(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for: " + portfolioId));
        
        List<Position> positions = positionRepository.findByPortfolioPortfolioId(portfolioId);

        Map<String, BigDecimal> allocationMap = new HashMap<>(); // using String to include "CASH"

        for (Position p : positions) {
            String assetType = p.getCompanyInfo().getAssetType().name();
            BigDecimal currentPrice = marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol()).getClosePrice();
            BigDecimal value = p.getTotalQuantity().multiply(currentPrice);
            allocationMap.merge(assetType, value, BigDecimal::add);
        }


        return positions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCompanyInfo().getAssetType(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                p -> p.getTotalQuantity().multiply(marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol()).getClosePrice()),
                                BigDecimal::add
                        )
                ))
                .entrySet().stream()
                .map(e -> new AssetAllocationDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<PositionDetailDTO> getPortfolioHoldings(String portfolioId) {
        List<Position> positions = positionRepository.findByPortfolioPortfolioId(portfolioId);

        return positions.stream().map(p -> {
            BigDecimal currentPrice = marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol()).getClosePrice();
            BigDecimal marketValue = p.getTotalQuantity().multiply(currentPrice);
            BigDecimal profitLoss = marketValue.subtract(p.getTotalQuantity().multiply(p.getAverageCost()));

            return PositionDetailDTO.builder()
                    .tickerSymbol(p.getCompanyInfo().getTickerSymbol())
                    .companyName(p.getCompanyInfo().getCompanyName())
                    .quantity(p.getTotalQuantity())
                    .averageCost(p.getAverageCost())
                    .currentPrice(currentPrice)
                    .marketValue(marketValue)
                    .profitLoss(profitLoss)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<HistoricalPerformanceDTO> getHistoricalPerformance(String portfolioId, int daysBack) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for: " + portfolioId));
        
        List<Position> positions = positionRepository.findByPortfolioPortfolioId(portfolioId);
        
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(daysBack);

        List<String> tickers = positions.stream()
                .map(p -> p.getCompanyInfo().getTickerSymbol())
                .collect(Collectors.toList());

        List<MarketData> historicalData = new ArrayList<>();
        if (!tickers.isEmpty()) {
            historicalData = marketDataRepository.findByTickerSymbolInAndTimestampBetweenOrderByTimestampAsc(tickers, startDate, endDate);
        }

        // Group market data by date & ticker
        Map<LocalDate, Map<String, BigDecimal>> dateTickerPriceMap = new TreeMap<>();
        
        for (MarketData md : historicalData) {
            LocalDate date = md.getTimestamp().toLocalDate();
            dateTickerPriceMap.putIfAbsent(date, new HashMap<>());
            // If multiple prices per day, keep the latest one , which is fine.
            dateTickerPriceMap.get(date).put(md.getTickerSymbol(), md.getClosePrice());
        }

        List<HistoricalPerformanceDTO> result = new ArrayList<>();
        
        // Assume cash is constant and holdings are constant
        BigDecimal cash = portfolio.getCashBalance();

        Map<String, BigDecimal> lastKnownPrices = new HashMap<>();
        for(Position p : positions) {
             try {
                 lastKnownPrices.put(p.getCompanyInfo().getTickerSymbol(), 
                        marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol()).getClosePrice());
             } catch(Exception e) {
                 lastKnownPrices.put(p.getCompanyInfo().getTickerSymbol(), BigDecimal.ZERO);
             }
        }

        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();

        while (!current.isAfter(end)) {
            Map<String, BigDecimal> dayPrices = dateTickerPriceMap.getOrDefault(current, new HashMap<>());
            
            // update last known prices
            for (Map.Entry<String, BigDecimal> entry : dayPrices.entrySet()) {
                lastKnownPrices.put(entry.getKey(), entry.getValue());
            }

            BigDecimal dailyHoldingsValue = BigDecimal.ZERO;
            for (Position p : positions) {
                BigDecimal price = lastKnownPrices.getOrDefault(p.getCompanyInfo().getTickerSymbol(), BigDecimal.ZERO);
                dailyHoldingsValue = dailyHoldingsValue.add(p.getTotalQuantity().multiply(price));
            }

            BigDecimal dailyTotalValue = cash.add(dailyHoldingsValue);
            result.add(new HistoricalPerformanceDTO(current.toString(), dailyTotalValue));

            current = current.plusDays(1);
        }

        return result;
    }
}
