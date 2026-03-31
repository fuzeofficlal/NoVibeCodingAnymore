package com.novibe.repository;

import com.novibe.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByPortfolioId(String portfolioId);
    void deleteByIdAndPortfolioId(Long id, String portfolioId);
}
