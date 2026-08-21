package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.model.Supplier;
import com.crisesmanagment.crisesmanagment.repo.SupplierRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

/**
 * Every supplier's price is derived live from a real published benchmark
 * (EIA's Europe Brent spot price — the benchmark Gulf/Nigerian/Indian crude
 * is actually priced off) plus a transparent, published-risk-driven
 * differential. Nothing here is carried forward from a previous run or from
 * the data.sql seed once a live fetch has succeeded — every refresh
 * recomputes every supplier's cost from scratch off the current live price.
 *
 * What IS reference data, not live data, and why:
 *   - Supplier name/country: real companies, but no public API exposes
 *     live per-company contract terms — that's proprietary trading data.
 *   - riskBaseline: a fixed geopolitical/logistics risk classification,
 *     used only as an input to the differential formula below.
 * Both are clearly labeled via Supplier.priceSource so the UI never
 * implies a number is "live" when it isn't.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private static final String SOURCE_LIVE = "EIA_LIVE_BRENT";
    private static final String SOURCE_FALLBACK = "STATIC_FALLBACK_SET_EIA_API_KEY";

    // $/barrel added per unit of risk_baseline (e.g. 0.10 risk -> +$2.00/bbl).
    // This models the real-world fact that crude from higher-risk routes
    // carries a freight/war-risk-insurance premium — it is a documented,
    // transparent formula, not a fabricated quote.
    private static final double RISK_PREMIUM_PER_UNIT = 20.0;

    @Qualifier("eiaWebClient")
    private final WebClient eiaWebClient;
    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${eia.api.key:}")
    private String eiaApiKey;

    private volatile Double lastLiveBrentPrice;
    private volatile Instant lastLiveFetchAt;

    // Refresh immediately on boot so the very first page load already
    // reflects live data (assuming the key is present), instead of waiting
    // up to 10 minutes for the scheduled run.
    @PostConstruct
    public void refreshOnStartup() {
        refreshOilPrices();
    }

    // EIA updates daily, so this is just polling for freshness, not hammering
    // a fast-moving feed.
    @Scheduled(fixedRate = 600000) // every 10 min
    public void refreshOilPrices() {
        if (eiaApiKey.isBlank()) {
            log.warn("EIA_API_KEY not set — leaving suppliers on STATIC_FALLBACK pricing");
            markAllAsFallback();
            return;
        }
        try {
            String response = eiaWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/petroleum/pri/spt/data/")
                            .queryParam("api_key", eiaApiKey)
                            .queryParam("frequency", "daily")
                            .queryParam("data[0]", "value")
                            .queryParam("facets[series][]", "RBRTE") // Europe Brent spot price FOB
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
                log.warn("EIA response had no price data — leaving previous prices in place");
                return;
            }
            double latestBrent = dataArray.get(0).path("value").asDouble();
            Instant now = Instant.now();

            // Recompute every supplier's cost from scratch off the live
            // price + its fixed risk classification. No dependency on
            // whatever the value happened to be before this call.
            for (Supplier s : supplierRepository.findAll()) {
                double risk = s.getRiskBaseline() != null ? s.getRiskBaseline() : 0d;
                double liveCost = latestBrent + (risk * RISK_PREMIUM_PER_UNIT);
                s.setBaseCostPerBarrel(liveCost);
                s.setPriceSource(SOURCE_LIVE);
                s.setLastPriceUpdate(now);
                supplierRepository.save(s);
            }

            lastLiveBrentPrice = latestBrent;
            lastLiveFetchAt = now;
            log.info("Refreshed all supplier costs from live EIA Brent spot price: {}", latestBrent);
        } catch (Exception e) {
            log.error("Failed to refresh oil prices from EIA — leaving previous prices in place", e);
        }
    }

    private void markAllAsFallback() {
        for (Supplier s : supplierRepository.findAll()) {
            if (s.getPriceSource() == null) {
                s.setPriceSource(SOURCE_FALLBACK);
                supplierRepository.save(s);
            }
        }
    }

    public Double getLastLiveBrentPrice() {
        return lastLiveBrentPrice;
    }

    public Instant getLastLiveFetchAt() {
        return lastLiveFetchAt;
    }
}