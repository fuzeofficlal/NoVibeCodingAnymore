package com.novibe.repository;

import com.novibe.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByPortfolioId(String portfolioId);
    void deleteByPortfolioIdAndTickerSymbol(String portfolioId, String tickerSymbol);
    boolean existsByPortfolioIdAndTickerSymbol(String portfolioId, String tickerSymbol);
}
