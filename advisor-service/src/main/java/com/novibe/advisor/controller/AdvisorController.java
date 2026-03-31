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

    public record ChatQueryRequest(String query) {}

    @PostMapping("/{portfolioId}/chat")
    public ResponseEntity<String> chatWithAgent(
            @PathVariable String portfolioId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader,
            @RequestBody ChatQueryRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest().body("Query cannot be empty.");
        }
        String response = advisorService.chatWithAgent(portfolioId, request.query(), apiKeyHeader);
        return ResponseEntity.ok(response);
    }

    public record ProactiveAlertRequest(String ticker, Double price, String type, Double target) {}

    @PostMapping("/{portfolioId}/proactive-alert")
    public ResponseEntity<String> triggerProactiveAlert(
            @PathVariable String portfolioId,
            @RequestBody ProactiveAlertRequest request) {
        
        String response = advisorService.generateProactiveAlert(portfolioId, request.ticker(), request.price(), request.type(), request.target());
        return ResponseEntity.ok(response);
    }
}
