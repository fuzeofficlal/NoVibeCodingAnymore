package com.novibe.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PositionDetailDTO {
    private String symbol;
    private String companyName;
    private BigDecimal shares;
    private BigDecimal currentPrice;
    private BigDecimal costBasis;
    private BigDecimal marketValue;
    private BigDecimal pl;
}
