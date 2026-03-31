package com.novibe.advisor.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AdvisorService {

    private final RestClient restClient;
    private final String defaultModel;
    private final String defaultBaseUrl;

    private static final String CORE_SERVICE_URL = "http://localhost:8080";

    // register agent toolsand functional calling ab
    private final org.springframework.ai.model.function.FunctionCallbackContext functionCallbackContext;
    
    private final String defaultApiKey;

    //  Memory Engine ---
    private final org.springframework.ai.chat.memory.ChatMemory chatMemory = new org.springframework.ai.chat.memory.InMemoryChatMemory();

    public AdvisorService(RestClient restClient,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.base-url}") String defaultBaseUrl,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.chat.options.model:gemini-3-pro-preview}") String defaultModel,
            @org.springframework.beans.factory.annotation.Value("${OPENAI_API_KEY:}") String defaultApiKey,
            org.springframework.beans.factory.ObjectProvider<org.springframework.ai.model.function.FunctionCallbackContext> functionCallbackProvider) {
        this.restClient = restClient;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
        this.defaultApiKey = defaultApiKey;
        this.functionCallbackContext = functionCallbackProvider.getIfAvailable();
    }

    public String chatWithAgent(String portfolioId, String query, String overrideApiKey) {
        String keyToUse = overrideApiKey;
        if (keyToUse == null || keyToUse.isBlank()) {
            keyToUse = this.defaultApiKey;
        }
        if (keyToUse == null || keyToUse.isBlank()) {
            return "**缺少 API 密钥**\n\n请求头缺少 `X-API-Key`，no 全局 API Key。";
        }

        var openAiApi = new org.springframework.ai.openai.api.OpenAiApi(this.defaultBaseUrl, keyToUse);
        var options = org.springframework.ai.openai.OpenAiChatOptions.builder().withModel(this.defaultModel).build();
        
        // 传递tooling calling ab
        var openAiChatModel = new org.springframework.ai.openai.OpenAiChatModel(openAiApi, options, this.functionCallbackContext, org.springframework.ai.retry.RetryUtils.DEFAULT_RETRY_TEMPLATE);
        
        ChatClient activeClient = ChatClient.create(openAiChatModel).mutate()
                .defaultSystem(
                        "You are an AI Agent running inside a quantitative finance system. You have tools at your disposal to fetch news, get portfolio summaries, and execute BUY/SELL transactions for the user. Always use these tools to fulfill User Requests. Never hallucinate data. The active user's portfolio ID is " + portfolioId + " .")
                .defaultFunctions("getMarketNews", "executeTransaction", "getPortfolioSummary", "getPortfolioHoldings", "getSmaIndicator")
                .defaultAdvisors(new org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor(this.chatMemory, portfolioId, 10))
                .build();

        return activeClient.prompt()
                .user(query)
                .call()
                .content();
    }

    public String analyzePortfolioRisk(String portfolioId, String overrideApiKey) {
        String keyToUse = overrideApiKey;
        if (keyToUse == null || keyToUse.isBlank()) {
            keyToUse = this.defaultApiKey;
        }
        if (keyToUse == null || keyToUse.isBlank()) {
            return " **缺少 API 密钥**\n\n请求头缺少 `X-API-Key`，无全局 API Key。";
        }

        var openAiApi = new org.springframework.ai.openai.api.OpenAiApi(this.defaultBaseUrl, keyToUse);
        var options = org.springframework.ai.openai.OpenAiChatOptions.builder().withModel(this.defaultModel).build();
        var openAiChatModel = new org.springframework.ai.openai.OpenAiChatModel(openAiApi, options, this.functionCallbackContext, org.springframework.ai.retry.RetryUtils.DEFAULT_RETRY_TEMPLATE);
        ChatClient activeClient = ChatClient.create(openAiChatModel).mutate()
                .defaultSystem(
                        "你是一位华尔街顶级量化基金的风险架构师与高级投资顾问。你的任务是收到用户的投资组合财务数据后，进行专业的评估。请以Markdown格式输出你的见解。分析必须包含：1.基于资产架构的风险评分；2.极端风险警告(黑天鹅/回调承受力)；3.对仓位管理的建议调整。语气需要极客、专业、且直击本质。请不要输出任何多余的寒暄。")
                .build();

        String summaryJson = "{}";
        try {
            summaryJson = restClient.get()
                    .uri(CORE_SERVICE_URL + "/api/v1/portfolios/{id}/summary", portfolioId)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            summaryJson = "{\"error\": \"Could not retrieve summary.\"}";
        }

        String holdingsJson = "[]";
        java.util.Set<String> allTickers = new java.util.HashSet<>();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        try {
            holdingsJson = restClient.get()
                    .uri(CORE_SERVICE_URL + "/api/v1/portfolios/{id}/holdings", portfolioId)
                    .retrieve()
                    .body(String.class);
            com.fasterxml.jackson.databind.JsonNode holdingsNode = mapper.readTree(holdingsJson);
            if (holdingsNode.isArray()) {
                holdingsNode.forEach(h -> {
                    if (h.has("tickerSymbol")) allTickers.add(h.get("tickerSymbol").asText());
                });
            }
        } catch (Exception e) {
            holdingsJson = "[]";
        }

        String watchlistJson = "[]";
        try {
            watchlistJson = restClient.get()
                    .uri(CORE_SERVICE_URL + "/api/v1/portfolios/{id}/watchlist", portfolioId)
                    .retrieve()
                    .body(String.class);
            com.fasterxml.jackson.databind.JsonNode watchlistNode = mapper.readTree(watchlistJson);
            if (watchlistNode.isArray()) {
                watchlistNode.forEach(w -> {
                    if (w.has("tickerSymbol")) allTickers.add(w.get("tickerSymbol").asText());
                });
            }
        } catch (Exception e) {
            watchlistJson = "[]";
        }

        String newsJson = "{}";
        if (!allTickers.isEmpty()) {
            String tickerQuery = String.join(",", allTickers);
            try {
                // calls to Python via internal Gateway port 8090 proxy or directly to 8000
                newsJson = restClient.get()
                        .uri("http://localhost:8090/api/v1/market/news?tickers=" + tickerQuery)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                newsJson = "{\"error\": \"News fetch failed.\"}";
            }
        }

        String combinedContext = String.format("{\"Portfolio_Summary\": %s, \"Current_Holdings\": %s, \"Watchlist_Assets\": %s, \"Latest_News_Stream\": %s}", 
                summaryJson, holdingsJson, watchlistJson, newsJson);

        String userPrompt = "以下是我目前的财务罗盘结构（持仓+自选池），以及相关股票最近几小时的市场突发新闻：" + combinedContext + "\n请为我生成一份详细的《量化智投风险洞察报告》，要求充分结合 Latest_News_Stream 的信息，评估个股面临的情绪面/消息面风险。";

        return activeClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    public String generateProactiveAlert(String portfolioId, String ticker, Double currentPrice, String alertType, Double targetPrice) {
        String keyToUse = this.defaultApiKey;
        if (keyToUse == null || keyToUse.isBlank()) {
            System.err.println("⚠️ **No API Key for Proactive Alert**");
            return "NO_API_KEY";
        }

        var openAiApi = new org.springframework.ai.openai.api.OpenAiApi(this.defaultBaseUrl, keyToUse);
        var options = org.springframework.ai.openai.OpenAiChatOptions.builder().withModel(this.defaultModel).build();
        var openAiChatModel = new org.springframework.ai.openai.OpenAiChatModel(openAiApi, options, this.functionCallbackContext, org.springframework.ai.retry.RetryUtils.DEFAULT_RETRY_TEMPLATE);
        
        org.springframework.ai.chat.client.ChatClient activeClient = org.springframework.ai.chat.client.ChatClient.create(openAiChatModel).mutate()
                .defaultSystem(
                        "You are an AI Agent running inside a quantitative finance system monitoring a user's portfolio (" + portfolioId + "). " +
                        "A serious backend system alert has been triggered. You MUST respond with a highly urgent, proactive warning message addressed to the user. " +
                        "Evaluate the situation and strongly suggest if they should sell or hold based on your general knowledge. Be brief and highly alarming.")
                .defaultFunctions("getMarketNews", "getSmaIndicator", "getPortfolioHoldings")
                .defaultAdvisors(new org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor(this.chatMemory, portfolioId, 10))
                .build();

        String alertPrompt = String.format("SYSTEM ALERT: The stock %s has hit a %s condition! Current Price: $%.2f. The user's target threshold was $%.2f. Please write an urgent proactive notification evaluating the news and suggesting immediate actions.",
            ticker, alertType, currentPrice, targetPrice);

        String urgentMessage = activeClient.prompt()
                .user(alertPrompt)
                .call()
                .content();
                
        System.out.println("================================================================");
        System.out.println(" \uD83D\uDEA8 [PROACTIVE AI COPILOT ALERT TRIGGERED FOR " + portfolioId + "]");
        System.out.println("================================================================");
        System.out.println(urgentMessage);
        System.out.println("================================================================\n");
        
        return urgentMessage;
    }
}
