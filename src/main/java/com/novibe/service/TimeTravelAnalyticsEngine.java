package com.novibe.service;

import com.novibe.dto.HistoricalPerformanceDTO;
import com.novibe.entity.MarketData;
import com.novibe.entity.Portfolio;
import com.novibe.entity.PortfolioTransaction;
import com.novibe.entity.Position;
import com.novibe.repository.MarketDataRepository;
import com.novibe.repository.PortfolioTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TimeTravelAnalyticsEngine {

    private final PortfolioTransactionRepository portfolioTransactionRepository;
    private final MarketDataRepository marketDataRepository;
    private final MarketDataService marketDataService;

    public List<HistoricalPerformanceDTO> calculateExactHistoricalPerformance(
            Portfolio portfolio, List<Position> currentPositions, int daysBack) {

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(daysBack);

        // 1. Fetch exact backward transactions (newest to oldest) down to startDate
        List<PortfolioTransaction> transactionsToRollback = portfolioTransactionRepository
                .findByPortfolioPortfolioIdAndTransactionDateGreaterThanEqualOrderByTransactionDateDesc(
                        portfolio.getPortfolioId(), startDate);

        // State trackers
        BigDecimal runningCash = portfolio.getCashBalance();
        Map<String, BigDecimal> runningPositions = new HashMap<>(); // Ticker -> Qty

        for (Position p : currentPositions) {
            runningPositions.put(p.getCompanyInfo().getTickerSymbol(), p.getTotalQuantity());
        }

        // STEP 1: The Rollback
        // Apply Reverse-Accounting to go back exactly to the start of "startDate"
        for (PortfolioTransaction txn : transactionsToRollback) {
            String type = txn.getTransactionType();
            BigDecimal txnAmount = txn.getQuantity().multiply(txn.getPricePerUnit());

            if ("BUY".equals(type)) {
                // Reverse BUY: Cash goes back UP, Stock goes DOWN
                runningCash = runningCash.add(txnAmount);
                String ticker = txn.getCompanyInfo().getTickerSymbol();
                BigDecimal currentQty = runningPositions.getOrDefault(ticker, BigDecimal.ZERO);
                runningPositions.put(ticker, currentQty.subtract(txn.getQuantity()));
            } else if ("SELL".equals(type)) {
                // Reverse SELL: Cash goes DOWN, Stock goes UP
                runningCash = runningCash.subtract(txnAmount);
                String ticker = txn.getCompanyInfo().getTickerSymbol();
                BigDecimal currentQty = runningPositions.getOrDefault(ticker, BigDecimal.ZERO);
                runningPositions.put(ticker, currentQty.add(txn.getQuantity()));
            } else if ("DEPOSIT".equals(type)) {
                // Reverse DEPOSIT: Cash goes DOWN
                runningCash = runningCash.subtract(txn.getQuantity()); // For cash flows, amount is built into quantity
            } else if ("WITHDRAW".equals(type)) {
                // Reverse WITHDRAW: Cash goes UP
                runningCash = runningCash.add(txn.getQuantity());
            }
        }

        // Prepare historical market data for quick lookup
        List<String> allTickersInHistory = new ArrayList<>(runningPositions.keySet());
        for (PortfolioTransaction txn : transactionsToRollback) {
            if (txn.getCompanyInfo() != null && !allTickersInHistory.contains(txn.getCompanyInfo().getTickerSymbol())) {
                allTickersInHistory.add(txn.getCompanyInfo().getTickerSymbol());
            }
        }

        List<MarketData> historicalData = new ArrayList<>();
        if (!allTickersInHistory.isEmpty()) {
            historicalData = marketDataRepository.findByTickerSymbolInAndTimestampBetweenOrderByTimestampAsc(
                    allTickersInHistory, startDate, endDate);
        }

        Map<LocalDate, Map<String, BigDecimal>> dateTickerPriceMap = new TreeMap<>();
        for (MarketData md : historicalData) {
            LocalDate date = md.getTimestamp().toLocalDate();
            dateTickerPriceMap.computeIfAbsent(date, k -> new HashMap<>())
                    .put(md.getTickerSymbol(), md.getClosePrice());
        }

        // Sort transactions forwards for standard replay timeline
        List<PortfolioTransaction> forwardTransactions = new ArrayList<>(transactionsToRollback);
        Collections.reverse(forwardTransactions); // Now oldest to newest

        Map<String, BigDecimal> lastKnownPrices = new HashMap<>();

        // STEP 2: The Forward Replay & Valuation
        List<HistoricalPerformanceDTO> result = new ArrayList<>();
        LocalDate currentDay = startDate.toLocalDate();
        LocalDate endDay = endDate.toLocalDate();

        int txnIndex = 0;

        while (!currentDay.isAfter(endDay)) {
            // Apply Standard Accounting for transactions that naturally occurred ON this specific day
            while (txnIndex < forwardTransactions.size()) {
                PortfolioTransaction txn = forwardTransactions.get(txnIndex);
                if (txn.getTransactionDate().toLocalDate().isAfter(currentDay)) {
                    break; // The transaction belongs to a future day
                }

                String type = txn.getTransactionType();
                BigDecimal txnAmount = txn.getQuantity().multiply(txn.getPricePerUnit());

                if ("BUY".equals(type)) {
                    runningCash = runningCash.subtract(txnAmount);
                    String ticker = txn.getCompanyInfo().getTickerSymbol();
                    runningPositions.put(ticker, runningPositions.getOrDefault(ticker, BigDecimal.ZERO).add(txn.getQuantity()));
                } else if ("SELL".equals(type)) {
                    runningCash = runningCash.add(txnAmount);
                    String ticker = txn.getCompanyInfo().getTickerSymbol();
                    runningPositions.put(ticker, runningPositions.getOrDefault(ticker, BigDecimal.ZERO).subtract(txn.getQuantity()));
                } else if ("DEPOSIT".equals(type)) {
                    runningCash = runningCash.add(txn.getQuantity());
                } else if ("WITHDRAW".equals(type)) {
                    runningCash = runningCash.subtract(txn.getQuantity());
                }
                txnIndex++;
            }

            // Valuation using End OF Day (EOD) MarketData
            Map<String, BigDecimal> expectedDayPrices = dateTickerPriceMap.getOrDefault(currentDay, new HashMap<>());
            
            // Update the rolling last known prices cache
            lastKnownPrices.putAll(expectedDayPrices);

            BigDecimal eodHoldingsValue = BigDecimal.ZERO;

            for (Map.Entry<String, BigDecimal> entry : runningPositions.entrySet()) {
                // If the holding size is exactly 0, ignore
                if (entry.getValue().compareTo(BigDecimal.ZERO) == 0) continue;

                // Fallback valuation if no history exist
                BigDecimal price = lastKnownPrices.get(entry.getKey());
                if (price == null) {
                    try {
                         price = marketDataService.getLatestPrice(entry.getKey()).getClosePrice();
                    } catch (Exception e) {
                         price = BigDecimal.ZERO;
                    }
                }
                eodHoldingsValue = eodHoldingsValue.add(entry.getValue().multiply(price));
            }

            BigDecimal eodTotalValue = runningCash.add(eodHoldingsValue);
            result.add(new HistoricalPerformanceDTO(currentDay.toString(), eodTotalValue));

            currentDay = currentDay.plusDays(1);
        }

        return result;
    }
}
