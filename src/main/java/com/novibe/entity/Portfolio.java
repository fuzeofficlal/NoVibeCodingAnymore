package com.novibe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Entity
@Table(name = "portfolio")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 50)
    private String portfolioId;

    @Column(nullable = false)
    private String name;

    @Column(name = "cash_balance", nullable = false)
    private BigDecimal cashBalance = BigDecimal.ZERO;

}
