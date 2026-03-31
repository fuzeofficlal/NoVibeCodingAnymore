package com.novibe.controller;

import com.novibe.dto.PriceAlertRequest;
import com.novibe.dto.WatchlistRequest;
import com.novibe.entity.PriceAlert;
import com.novibe.entity.Watchlist;
import com.novibe.service.PortfolioFeaturesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}")
@RequiredArgsConstructor
public class PortfolioFeaturesController {

    private final PortfolioFeaturesService featuresService;

    @GetMapping("/watchlist")
    public ResponseEntity<List<Watchlist>> getWatchlist(@PathVariable String portfolioId) {
        return ResponseEntity.ok(featuresService.getWatchlist(portfolioId));
    }

    @PostMapping("/watchlist")
    public ResponseEntity<Watchlist> addWatchlist(@PathVariable String portfolioId, @RequestBody WatchlistRequest request) {
        return ResponseEntity.ok(featuresService.addWatchlist(portfolioId, request));
    }

    @DeleteMapping("/watchlist/{ticker}")
    public ResponseEntity<Void> removeWatchlist(@PathVariable String portfolioId, @PathVariable String ticker) {
        featuresService.removeWatchlist(portfolioId, ticker);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<PriceAlert>> getAlerts(@PathVariable String portfolioId) {
        return ResponseEntity.ok(featuresService.getAlerts(portfolioId));
    }

    @PostMapping("/alerts")
    public ResponseEntity<PriceAlert> addAlert(@PathVariable String portfolioId, @RequestBody PriceAlertRequest request) {
        return ResponseEntity.ok(featuresService.addAlert(portfolioId, request));
    }

    @DeleteMapping("/alerts/{alertId}")
    public ResponseEntity<Void> removeAlert(@PathVariable String portfolioId, @PathVariable Long alertId) {
        featuresService.removeAlert(portfolioId, alertId);
        return ResponseEntity.ok().build();
    }
}
