package com.novibe.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class MarketDataId implements Serializable {

    private String tickerSymbol;
    private LocalDateTime timestamp;
}
