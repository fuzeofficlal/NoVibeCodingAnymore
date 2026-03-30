package com.novibe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.math.BigDecimal;

@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 50)
    private String portfolioId;

    @Column(nullable = false)
    private String name;

    @Column(name = "cash_balance", nullable = false)
    private BigDecimal cashBalance = BigDecimal.ZERO;

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }
}
