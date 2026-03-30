package com.novibe.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CashFlowRequest {
    private String type; // DEPOSIT, WITHDRAW
    private BigDecimal amount;
}
