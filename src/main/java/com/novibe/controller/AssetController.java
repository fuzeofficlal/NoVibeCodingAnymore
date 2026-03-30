package com.novibe.controller;

import com.novibe.dto.AssetDTO;
import com.novibe.entity.CompanyInfo;
import com.novibe.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final MarketDataService marketDataService;

    @GetMapping
    public ResponseEntity<List<AssetDTO>> getAvailableAssets() {
        List<CompanyInfo> companies = marketDataService.getAvailableAssets();
        
        List<AssetDTO> dtoList = companies.stream().map(c -> 
            AssetDTO.builder()
                .symbol(c.getTickerSymbol())
                .name(c.getCompanyName())
                .type(c.getAssetType().name())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }
}
