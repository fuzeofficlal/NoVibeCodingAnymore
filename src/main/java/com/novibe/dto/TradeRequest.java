package com.novibe.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TradeRequest {
    private String tickerSymbol;
    private String transactionType; // BUY, SELL, DEPOSIT, WITHDRAW
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
}
