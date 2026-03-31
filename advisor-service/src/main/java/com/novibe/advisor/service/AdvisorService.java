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

    public AdvisorService(RestClient restClient,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.base-url}") String defaultBaseUrl,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.chat.options.model:gemini-3-pro-preview}") String defaultModel) {
        this.restClient = restClient;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
    }

    public String analyzePortfolioRisk(String portfolioId, String overrideApiKey) {
        if (overrideApiKey == null || overrideApiKey.isBlank()) {
            return "⚠️ **缺少 API 密钥**\n\n请求头需要配置 `X-API-Key` 。Public的项目公开硬编码Key等死吧";
        }

        var openAiApi = new org.springframework.ai.openai.api.OpenAiApi(this.defaultBaseUrl, overrideApiKey);
        var options = org.springframework.ai.openai.OpenAiChatOptions.builder().withModel(this.defaultModel).build();
        var openAiChatModel = new org.springframework.ai.openai.OpenAiChatModel(openAiApi, options);
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
        try {
            holdingsJson = restClient.get()
                    .uri(CORE_SERVICE_URL + "/api/v1/portfolios/{id}/holdings", portfolioId)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            holdingsJson = "[]";
        }

        String combinedContext = String.format("{\"Portfolio_Summary\": %s, \"Current_Holdings\": %s}", summaryJson,
                holdingsJson);
        String userPrompt = "以下是我目前的持仓快照与账户资金罗盘：" + combinedContext + "\n请为我生成一份详细的《量化智投风险洞察报告》。";

        return activeClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }
}
