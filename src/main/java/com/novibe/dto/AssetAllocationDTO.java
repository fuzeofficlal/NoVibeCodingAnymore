package com.novibe.dto;

import com.novibe.entity.AssetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AssetAllocationDTO {
    private AssetType assetType;
    private BigDecimal totalValue;
}
