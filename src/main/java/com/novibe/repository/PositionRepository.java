package com.novibe.repository;

import com.novibe.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, String> {
    //  (GET /api/portfolios/{id}/positions) 获取当前持仓列表
    List<Position> findByPortfolioPortfolioId(String portfolioId);
}
