package com.novibe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "company_info")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CompanyInfo {

    @Id
    @Column(name = "ticker_symbol", length = 50)
    private String tickerSymbol;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "asset_type", nullable = false, length = 20)
    private String assetType = "STOCK";
}
