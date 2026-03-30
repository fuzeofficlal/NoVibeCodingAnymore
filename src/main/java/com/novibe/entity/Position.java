package com.novibe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "position")
public class Position {

    @Id
    @Column(name = "position_id", length = 50)
    private String positionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_symbol", nullable = false)
    private CompanyInfo companyInfo;

    @Column(name = "total_quantity", nullable = false)
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @Column(name = "average_cost", nullable = false)
    private BigDecimal averageCost = BigDecimal.ZERO;


}
