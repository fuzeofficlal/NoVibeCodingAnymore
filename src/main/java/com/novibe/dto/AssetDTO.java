package com.novibe.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetDTO {
    private String symbol;
    private String name;
    private String type;
}
