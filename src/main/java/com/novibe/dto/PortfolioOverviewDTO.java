package com.novibe.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PortfolioOverviewDTO {
    private BigDecimal cashBalance;
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private BigDecimal totalReturnPercentage;
}
