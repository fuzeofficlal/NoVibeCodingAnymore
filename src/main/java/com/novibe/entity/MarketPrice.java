package com.novibe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "market_price")
public class MarketPrice {

    @Id
    @Column(name = "ticker_symbol", length = 50)
    private String tickerSymbol;

    @Column(name = "current_price", nullable = false)
    private BigDecimal currentPrice;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

}
