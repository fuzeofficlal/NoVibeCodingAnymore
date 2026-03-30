package com.novibe.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class PortfolioOverviewDTO {
    private BigDecimal totalPortfolioValue;
    private BigDecimal totalReturnPercentage;
    private Map<String, BigDecimal> allocation;
}
