package com.novibe.repository;

import com.novibe.entity.PortfolioTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, String> {
    // (GET /api/portfolios/{id}/transactions) 暂定 获取交易流水 by id
    List<PortfolioTransaction> findByPortfolioPortfolioIdOrderByTransactionDateDesc(String portfolioId);

    // 获取特定时间后的所有交易记录，按时间倒序排列（用于反向时光推演）
    List<PortfolioTransaction> findByPortfolioPortfolioIdAndTransactionDateGreaterThanEqualOrderByTransactionDateDesc(String portfolioId, java.time.LocalDateTime startDate);
}
