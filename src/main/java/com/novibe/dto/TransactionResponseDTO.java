package com.novibe.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TransactionResponseDTO {
    private String message;
    private String transactionId;
    private BigDecimal newCashBalance;
}
