package com.novibe.service;

import com.novibe.dto.PriceAlertRequest;
import com.novibe.dto.WatchlistRequest;
import com.novibe.entity.PriceAlert;
import com.novibe.entity.Watchlist;
import com.novibe.repository.PriceAlertRepository;
import com.novibe.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioFeaturesService {

    private final WatchlistRepository watchlistRepository;
    private final PriceAlertRepository priceAlertRepository;

    public List<Watchlist> getWatchlist(String portfolioId) {
        return watchlistRepository.findByPortfolioId(portfolioId);
    }

    @Transactional
    public Watchlist addWatchlist(String portfolioId, WatchlistRequest request) {
        if (watchlistRepository.existsByPortfolioIdAndTickerSymbol(portfolioId, request.getTickerSymbol())) {
            throw new IllegalArgumentException("Ticker already in watchlist.");
        }
        Watchlist entry = new Watchlist(portfolioId, request.getTickerSymbol());
        return watchlistRepository.save(entry);
    }

    @Transactional
    public void removeWatchlist(String portfolioId, String tickerSymbol) {
        watchlistRepository.deleteByPortfolioIdAndTickerSymbol(portfolioId, tickerSymbol);
    }

    public List<PriceAlert> getAlerts(String portfolioId) {
        return priceAlertRepository.findByPortfolioId(portfolioId);
    }

    @Transactional
    public PriceAlert addAlert(String portfolioId, PriceAlertRequest request) {
        PriceAlert alert = new PriceAlert(
                portfolioId,
                request.getTickerSymbol(),
                request.getTargetPrice(),
                request.getAlertType()
        );
        return priceAlertRepository.save(alert);
    }

    @Transactional
    public void removeAlert(String portfolioId, Long alertId) {
        priceAlertRepository.deleteByIdAndPortfolioId(alertId, portfolioId);
    }
}
