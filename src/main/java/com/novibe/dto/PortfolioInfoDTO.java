package com.novibe.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PortfolioInfoDTO {
    private String portfolioId;
    private String name;
    private BigDecimal cashBalance;
}
