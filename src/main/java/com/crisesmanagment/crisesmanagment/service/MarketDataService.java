package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.model.Supplier;
import com.crisesmanagment.crisesmanagment.repo.SupplierRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    @Qualifier("eiaWebClient")
    private final WebClient eiaWebClient;
    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${eia.api.key:}")
    private String eiaApiKey;

    // Tracks the last WTI baseline we applied, so each supplier's spread
    // (premium/discount vs WTI) can be preserved on the next refresh instead
    // of being recomputed against a stale baseline.
    private double lastKnownBaseline = 75.0; // matches your data.sql seed baseline

    // EIA updates daily, so this is just polling for freshness, not hammering
    // a fast-moving feed.
    @Scheduled(fixedRate = 600000) // every 10 min
    public void refreshOilPrices() {
        if (eiaApiKey.isBlank()) {
            log.warn("EIA_API_KEY not set — skipping oil price refresh");
            return;
        }
        try {
            String response = eiaWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/petroleum/pri/spt/data/")
                            .queryParam("api_key", eiaApiKey)
                            .queryParam("frequency", "daily")
                            .queryParam("data[0]", "value")
                            .queryParam("facets[series][]", "RWTC") // WTI Cushing spot price
                            .queryParam("sort[0][column]", "period")
                            .queryParam("sort[0][direction]", "desc")
                            .queryParam("length", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode dataArray = root.path("response").path("data");
            if (!dataArray.isArray() || dataArray.isEmpty()) {
                log.warn("EIA response had no price data");
                return;
            }
            double latestWti = dataArray.get(0).path("value").asDouble();

            // Apply WTI as the new baseline, but keep each supplier's original
            // spread (can be negative — e.g. Iraq/Kuwait/Nigeria priced below
            // baseline) instead of clamping it to zero.
            for (Supplier s : supplierRepository.findAll()) {
                double currentSpread = s.getBaseCostPerBarrel() - lastKnownBaseline;
                s.setBaseCostPerBarrel(latestWti + currentSpread);
                supplierRepository.save(s);
            }

            lastKnownBaseline = latestWti;
            log.info("Refreshed supplier costs from EIA WTI spot price: {}", latestWti);
        } catch (Exception e) {
            log.error("Failed to refresh oil prices from EIA", e);
        }
    }
}