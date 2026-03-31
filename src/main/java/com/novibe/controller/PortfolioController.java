package com.novibe.controller;

import com.novibe.dto.HistoricalPerformanceDTO;
import com.novibe.dto.PortfolioInfoDTO;
import com.novibe.dto.PortfolioOverviewDTO;
import com.novibe.dto.PositionDetailDTO;
import com.novibe.dto.CreatePortfolioRequest;
import com.novibe.dto.TransactionHistoryDTO;
import com.novibe.entity.Portfolio;
import com.novibe.exception.ResourceNotFoundException;
import com.novibe.repository.PortfolioRepository;
import com.novibe.service.PortfolioAnalyticsService;
import com.novibe.service.PortfolioManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioAnalyticsService analyticsService;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioManagementService portfolioManagementService;

    @PostMapping
    public ResponseEntity<PortfolioInfoDTO> createPortfolio(@RequestBody(required = false) CreatePortfolioRequest request) {
        if (request == null) request = new CreatePortfolioRequest();
        PortfolioInfoDTO newPortfolioInfo = portfolioManagementService.createPortfolio(request);
        return new ResponseEntity<>(newPortfolioInfo, org.springframework.http.HttpStatus.CREATED);
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioInfoDTO> getPortfolioInfo(@PathVariable String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));

        PortfolioInfoDTO info = PortfolioInfoDTO.builder()
                .portfolioId(portfolio.getPortfolioId())
                .name(portfolio.getName())
                .cashBalance(portfolio.getCashBalance())
                .build();
        return ResponseEntity.ok(info);
    }

    @GetMapping("/{portfolioId}/summary")
    public ResponseEntity<PortfolioOverviewDTO> getSummary(@PathVariable String portfolioId) {
        return ResponseEntity.ok(analyticsService.getPortfolioOverview(portfolioId));
    }

    @GetMapping("/{portfolioId}/holdings")
    public ResponseEntity<List<PositionDetailDTO>> getHoldings(@PathVariable String portfolioId) {
        return ResponseEntity.ok(analyticsService.getPortfolioHoldings(portfolioId));
    }

    @GetMapping("/{portfolioId}/performance")
    public ResponseEntity<List<HistoricalPerformanceDTO>> getPerformance(
            @PathVariable String portfolioId,
            @RequestParam(defaultValue = "1M") String range) {

        int daysBack = 30; // default 1M
        if ("1M".equalsIgnoreCase(range))
            daysBack = 30;
        else if ("3M".equalsIgnoreCase(range))
            daysBack = 90;
        else if ("6M".equalsIgnoreCase(range))
            daysBack = 180;
        else if ("YTD".equalsIgnoreCase(range))
            daysBack = java.time.LocalDate.now().getDayOfYear();
        else if ("1Y".equalsIgnoreCase(range))
            daysBack = 365;

        return ResponseEntity.ok(analyticsService.getHistoricalPerformance(portfolioId, daysBack));
    }

    @GetMapping("/{portfolioId}/transactions")
    public ResponseEntity<List<TransactionHistoryDTO>> getTransactions(@PathVariable String portfolioId) {
        return ResponseEntity.ok(portfolioManagementService.getTransactionHistory(portfolioId));
    }
}
