package com.novibe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
@Table(name = "market_data")
@IdClass(MarketDataId.class)
public class MarketData {

    @Id
    @Column(name = "ticker_symbol", length = 50)
    private String tickerSymbol;

    @Id
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "close_price", nullable = false)
    private BigDecimal closePrice;


}
