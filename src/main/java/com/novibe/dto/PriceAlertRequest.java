package com.novibe.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PriceAlertRequest {
    private String tickerSymbol;
    private BigDecimal targetPrice;
    private String alertType;
}
