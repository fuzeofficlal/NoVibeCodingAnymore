package com.novibe.controller;

import com.novibe.service.YahooFinanceSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final YahooFinanceSyncService syncService;

    /**
     * 触发从指定日期开始的标普500数据同步任务（后台异步执行）
     * 请求示例：POST /api/market-data/sp500/sync?startDate=2024-01-01
     */
    @PostMapping("/sp500/sync")
    public ResponseEntity<String> syncSp500(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        
        syncService.syncSp500Data(startDate);
        return ResponseEntity.ok("SP500 Sync Job started from " + startDate + ". Please check server logs for progress.");
    }
}
