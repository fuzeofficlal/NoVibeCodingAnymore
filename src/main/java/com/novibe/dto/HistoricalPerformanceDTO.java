package com.novibe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class HistoricalPerformanceDTO {
    private String date; // format YYYY-MM-DD
    private BigDecimal value;
}
