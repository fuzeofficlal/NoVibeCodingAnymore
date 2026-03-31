package com.novibe.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alert")
@Data
@NoArgsConstructor
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private String portfolioId;

    @Column(name = "ticker_symbol", nullable = false)
    private String tickerSymbol;

    @Column(name = "target_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal targetPrice;

    @Column(name = "alert_type", nullable = false, length = 20)
    private String alertType; // STOPLOSS type  or TAKEPROFIT type

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public PriceAlert(String portfolioId, String tickerSymbol, BigDecimal targetPrice, String alertType) {
        this.portfolioId = portfolioId;
        this.tickerSymbol = tickerSymbol;
        this.targetPrice = targetPrice;
        this.alertType = alertType;
    }
}
