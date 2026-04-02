package com.novibe.advisor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

@Configuration
public class AgentTools {

    private final RestClient restClient;
    private final String coreUrl;
    private final String gatewayUrl;

    public AgentTools(RestClient restClient,
            @org.springframework.beans.factory.annotation.Value("${CORE_URL:http://localhost:8080}") String coreUrl,
            @org.springframework.beans.factory.annotation.Value("${GATEWAY_URL:http://localhost:8090}") String gatewayUrl) {
        this.restClient = restClient;
        this.coreUrl = coreUrl;
        this.gatewayUrl = gatewayUrl;
    }

    public record MarketNewsRequest(String tickers) {}

    @Bean
    @Description("Get the latest real-time financial news for one or multiple stock tickers (e.g., AAPL).")
    public Function<MarketNewsRequest, String> getMarketNews() {
        return request -> {
            try {
                return restClient.get()
                    .uri(gatewayUrl + "/api/v1/market/news?tickers=" + request.tickers())
                    .retrieve()
                    .body(String.class);
            } catch(Exception e) {
                return "Failed to fetch news: " + e.getMessage();
            }
        };
    }

    public record TransactionActionRequest(String portfolioId, String transactionType, String tickerSymbol, Double quantity) {}

    @Bean
    @Description("Execute a BUY or SELL transaction for a specific portfolio. The transactionType must be 'BUY' or 'SELL'.")
    public Function<TransactionActionRequest, String> executeTransaction() {
        return request -> {
            try {
                // Fetch current price
                String priceJson = restClient.get()
                    .uri(gatewayUrl + "/api/v1/market/prices?tickers=" + request.tickerSymbol())
                    .retrieve()
                    .body(String.class);
                
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(priceJson);
                double currentPrice = 0.0;
                if(root.isArray() && root.size() > 0) {
                    currentPrice = root.get(0).get("current_price").asDouble();
                } else {
                    return "Error: Could not find current market price for " + request.tickerSymbol() + ".";
                }

                // Execute the transaction
                String payload = String.format("{\"transactionType\":\"%s\",\"tickerSymbol\":\"%s\",\"quantity\":%s,\"pricePerUnit\":%s}", 
                        request.transactionType().toUpperCase(), request.tickerSymbol(), request.quantity(), currentPrice);
                
                restClient.post()
                    .uri(coreUrl + "/api/v1/portfolios/{id}/transactions", request.portfolioId())
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .body(String.class);
                
                return "Successfully executed " + request.transactionType() + " for " + request.quantity() + " shares of " + request.tickerSymbol() + ".";
            } catch(Exception e) {
                return "Transaction failed: " + e.getMessage();
            }
        };
    }

    public record PortfolioSummaryRequest(String portfolioId) {}

    @Bean
    @Description("Get the net asset value, ROI, and summary allocation of a user's portfolio.")
    public Function<PortfolioSummaryRequest, String> getPortfolioSummary() {
        return request -> {
            try {
                return restClient.get()
                    .uri(coreUrl + "/api/v1/portfolios/{id}/summary", request.portfolioId())
                    .retrieve()
                    .body(String.class);
            } catch(Exception e) {
                return "Failed to fetch summary: " + e.getMessage();
            }
        };
    }

    public record PortfolioHoldingsRequest(String portfolioId) {}

    @Bean
    @Description("Get the exact list of individual stocks, cryptos, and their quantities held in the user's portfolio.")
    public Function<PortfolioHoldingsRequest, String> getPortfolioHoldings() {
        return request -> {
            try {
                return restClient.get()
                    .uri(coreUrl + "/api/v1/portfolios/{id}/holdings", request.portfolioId())
                    .retrieve()
                    .body(String.class);
            } catch(Exception e) {
                return "Failed to fetch holdings: " + e.getMessage();
            }
        };
    }

    public record SmaIndicatorRequest(String tickerSymbol, Integer days) {}

    @Bean
    @Description("Get the Simple Moving Average (SMA) technical indicator charting data for a stock over a given number of days (e.g. 20, 50, 200).")
    public Function<SmaIndicatorRequest, String> getSmaIndicator() {
        return request -> {
            try {
                int defaultDays = request.days() != null ? request.days() : 20;
                return restClient.get()
                    .uri(gatewayUrl + "/api/v1/market/indicators/sma/{ticker}?days={days}", request.tickerSymbol(), defaultDays)
                    .retrieve()
                    .body(String.class);
            } catch(Exception e) {
                return "Failed to fetch SMA: " + e.getMessage();
            }
        };
    }
}
