package com.novibe.service;

import com.novibe.dto.AssetAllocationDTO;
import com.novibe.dto.HistoricalPerformanceDTO;
import com.novibe.dto.PortfolioOverviewDTO;
import com.novibe.dto.PositionDetailDTO;
import com.novibe.entity.MarketData;
import com.novibe.entity.Portfolio;
import com.novibe.entity.Position;
import com.novibe.exception.ResourceNotFoundException;
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
    private final TimeTravelAnalyticsEngine timeTravelEngine;

    public PortfolioOverviewDTO getPortfolioOverview(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for: " + portfolioId));

        List<Position> positions = positionRepository.findByPortfolioPortfolioId(portfolioId);

        BigDecimal cashBalance = portfolio.getCashBalance();
        BigDecimal holdingsValue = BigDecimal.ZERO;
        BigDecimal holdingsCost = BigDecimal.ZERO;

        for (Position p : positions) {
            BigDecimal currentPrice = marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol())
                    .getClosePrice();
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

        Map<String, BigDecimal> allocation = new HashMap<>(); // using String to include "CASH"
        allocation.put("CASH", cashBalance);
        for (Position p : positions) {
            String assetType = p.getCompanyInfo().getAssetType().name();
            BigDecimal currentPrice = marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol())
                    .getClosePrice();
            BigDecimal value = p.getTotalQuantity().multiply(currentPrice);
            allocation.merge(assetType, value, BigDecimal::add);
        }

        return PortfolioOverviewDTO.builder()
                .totalPortfolioValue(totalValue)
                .totalReturnPercentage(totalReturnPercentage)
                .allocation(allocation)
                .build();
    }

    public List<AssetAllocationDTO> getAssetAllocationSummary(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for: " + portfolioId));

        List<Position> positions = positionRepository.findByPortfolioPortfolioId(portfolioId);

        Map<String, BigDecimal> allocationMap = new HashMap<>(); // using String to include "CASH"

        for (Position p : positions) {
            String assetType = p.getCompanyInfo().getAssetType().name();
            BigDecimal currentPrice = marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol())
                    .getClosePrice();
            BigDecimal value = p.getTotalQuantity().multiply(currentPrice);
            allocationMap.merge(assetType, value, BigDecimal::add);
        }

        return positions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCompanyInfo().getAssetType(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                p -> p.getTotalQuantity()
                                        .multiply(marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol())
                                                .getClosePrice()),
                                BigDecimal::add)))
                .entrySet().stream()
                .map(e -> new AssetAllocationDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<PositionDetailDTO> getPortfolioHoldings(String portfolioId) {
        List<Position> positions = positionRepository.findByPortfolioPortfolioId(portfolioId);

        return positions.stream().map(p -> {
            BigDecimal currentPrice = marketDataService.getLatestPrice(p.getCompanyInfo().getTickerSymbol())
                    .getClosePrice();
            BigDecimal marketValue = p.getTotalQuantity().multiply(currentPrice);
            BigDecimal profitLoss = marketValue.subtract(p.getTotalQuantity().multiply(p.getAverageCost()));

            return PositionDetailDTO.builder()
                    .symbol(p.getCompanyInfo().getTickerSymbol())
                    .companyName(p.getCompanyInfo().getCompanyName())
                    .shares(p.getTotalQuantity())
                    .costBasis(p.getAverageCost())
                    .currentPrice(currentPrice)
                    .marketValue(marketValue)
                    .pl(profitLoss)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<HistoricalPerformanceDTO> getHistoricalPerformance(String portfolioId, int daysBack) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for: " + portfolioId));
        
        List<Position> positions = positionRepository.findByPortfolioPortfolioId(portfolioId);
        
        return timeTravelEngine.calculateExactHistoricalPerformance(portfolio, positions, daysBack);
    }
}
