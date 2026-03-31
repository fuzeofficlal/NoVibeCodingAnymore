package com.novibe.advisor.controller;

import com.novibe.advisor.service.AdvisorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/advisor")
public class AdvisorController {

    private final AdvisorService advisorService;

    public AdvisorController(AdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @GetMapping("/{portfolioId}/insight")
    public ResponseEntity<String> getPortfolioInsight(
            @PathVariable String portfolioId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {
        String insightReport = advisorService.analyzePortfolioRisk(portfolioId, apiKeyHeader);
        return ResponseEntity.ok(insightReport);
    }
}
