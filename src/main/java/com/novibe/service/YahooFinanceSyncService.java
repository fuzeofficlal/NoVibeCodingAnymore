package com.novibe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novibe.entity.MarketData;
import com.novibe.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YahooFinanceSyncService {

    private final MarketDataRepository marketDataRepository;
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${yahoo.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${yahoo.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${yahoo.proxy.port:7890}")
    private int proxyPort;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        if (proxyEnabled) {
            log.info("Enabling HTTP Proxy for Yahoo Finance: {}:{}", proxyHost, proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
        }
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 读取 sp500_cache.json 里面的数据，异步增量抓取标普 500 所有成分股
     */
    @Async
    public void syncSp500Data(LocalDate startDate) {
        log.info("Starting SP500 Sync from {}", startDate);
        long startTs = startDate.atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond();
        long endTs = Instant.now().getEpochSecond();
        
        try {
            // 读取根目录下的 sp500_cache.json 文件
            File cacheFile = new File("sp500_cache.json");
            if (!cacheFile.exists()) {
                log.error("sp500_cache.json 文件未找到!");
                return;
            }
            
            JsonNode rootNode = objectMapper.readTree(cacheFile);
            if (rootNode.isArray()) {
                for (JsonNode entry : rootNode) {
                    // entry 类似于 ["MMM", "3M"]
                    if (entry.isArray() && entry.size() >= 2) {
                        String ticker = entry.get(0).asText();
                        syncTicker(ticker, startTs, endTs);
                        // 增加休眠时间至 2000 毫秒，防止并发太快触发雅虎的反爬虫 429 报错
                        Thread.sleep(2000); 
                    }
                }
            }
            log.info("SP500 Sync completed successfully!");
        } catch (Exception e) {
            log.error("Failed to sync SP500 data", e);
        }
    }

    /**
     * 拉取并保存单个 Ticker 的历史数据
     */
    public void syncTicker(String ticker, long startTs, long endTs) {
        String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d", 
                ticker, startTs, endTs);
        
        try {
            log.debug("Fetching market data for {}", ticker);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                log.warn("Failed to fetch data for {}: HTTP {}", ticker, response.getStatusCode());
                return;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode result = root.path("chart").path("result").get(0);
            if (result == null || result.isMissingNode()) {
                log.warn("No result block found for {}", ticker);
                return;
            }

            JsonNode timestamps = result.path("timestamp");
            JsonNode closePrices = result.path("indicators").path("quote").get(0).path("close");

            if (timestamps.isMissingNode() || closePrices.isMissingNode() || !timestamps.isArray() || !closePrices.isArray()) {
                log.warn("Missing timestamp or close data for {}", ticker);
                return;
            }

            List<MarketData> listToSave = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                if (closePrices.get(i).isNull()) {
                    continue; // 跳过休市或者无价格的数据点
                }

                long ts = timestamps.get(i).asLong();
                BigDecimal closePrice = new BigDecimal(closePrices.get(i).asText());
                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.of("America/New_York"));

                MarketData data = new MarketData(ticker, dateTime, closePrice);
                listToSave.add(data);
            }

            if (!listToSave.isEmpty()) {
                marketDataRepository.saveAll(listToSave);
                log.info("Saved {} records for {}", listToSave.size(), ticker);
            }

        } catch (Exception e) {
            log.error("Error fetching/saving data for {}: {}", ticker, e.getMessage());
        }
    }
}
