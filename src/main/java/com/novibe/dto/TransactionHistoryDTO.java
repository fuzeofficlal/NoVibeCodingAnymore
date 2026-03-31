package com.novibe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryDTO {
    private String transactionId;
    private String transactionType;
    private String tickerSymbol;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
    private LocalDateTime transactionDate;
}
