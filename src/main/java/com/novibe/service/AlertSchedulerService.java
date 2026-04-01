package com.novibe.service;

import com.novibe.entity.MarketPrice;
import com.novibe.entity.PriceAlert;
import com.novibe.repository.MarketPriceRepository;
import com.novibe.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertSchedulerService {

    private final PriceAlertRepository priceAlertRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final RestClient restClient = RestClient.create();

    @Value("${advisor.service.url:http://localhost:8081}")
    private String advisorServiceUrl;

    @Scheduled(fixedDelay = 20000)
    public void scanAndTriggerAlerts() {
        log.info("[AlertScheduler] Scanning active Price Alerts...");
        List<PriceAlert> alerts = priceAlertRepository.findAll();

        if (alerts.isEmpty()) {
            return;
        }

        for (PriceAlert alert : alerts) {
            String ticker = alert.getTickerSymbol();
            marketPriceRepository.findById(ticker).ifPresent(marketPrice -> {
                boolean triggered = false;

                if ("TAKE_PROFIT".equalsIgnoreCase(alert.getAlertType()) &&
                        marketPrice.getCurrentPrice().compareTo(alert.getTargetPrice()) >= 0) {
                    triggered = true;
                } else if ("STOP_LOSS".equalsIgnoreCase(alert.getAlertType()) &&
                        marketPrice.getCurrentPrice().compareTo(alert.getTargetPrice()) <= 0) {
                    triggered = true;
                }

                if (triggered) {
                    log.warn("[AlertScheduler] 🚨 TRIGGERED: {} hit {} target of {}",
                            ticker, alert.getAlertType(), alert.getTargetPrice());

                    // notify
                    try {
                        String payload = String.format(
                                "{\"ticker\": \"%s\", \"price\": %s, \"type\": \"%s\", \"target\": %s}",
                                ticker, marketPrice.getCurrentPrice(), alert.getAlertType(), alert.getTargetPrice());

                        restClient.post()
                                .uri(advisorServiceUrl + "/api/v1/advisor/{id}/proactive-alert", alert.getPortfolioId())
                                .header("Content-Type", "application/json")
                                .body(payload)
                                .retrieve()
                                .toBodilessEntity();

                        priceAlertRepository.delete(alert);
                    } catch (Exception e) {
                        log.error("[AlertScheduler] Failed to notify AI Copilot: {}", e.getMessage());
                    }
                }
            });
        }
    }
}
