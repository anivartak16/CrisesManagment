package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-stop status panel for every external integration this app depends on,
 * so it's obvious at a glance which data on screen is live vs. fallback/
 * unconfigured, without having to check each feature's own status endpoint.
 */
@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

    private final MarketDataService marketDataService;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @GetMapping
    public Map<String, Object> getStatus() {
        List<Map<String, Object>> integrations = new ArrayList<>();

        Double brent = marketDataService.getLastLiveBrentPrice();
        Instant fetchedAt = marketDataService.getLastLiveFetchAt();
        integrations.add(integration(
                "EIA (oil pricing)",
                brent != null,
                brent != null ? "Live Brent spot pricing" : "Set EIA_API_KEY — using static fallback pricing",
                fetchedAt != null ? fetchedAt.toString() : null
        ));

        boolean geminiConfigured = geminiApiKey != null && !geminiApiKey.isBlank();
        boolean groqConfigured = groqApiKey != null && !groqApiKey.isBlank();
        integrations.add(integration(
                "Gemini (headline extraction)",
                geminiConfigured,
                geminiConfigured
                        ? (groqConfigured
                                ? "Configured — cross-checking every headline against Groq"
                                : "Configured — auto-classifying news headlines")
                        : "Set GEMINI_API_KEY — automatic risk-event extraction disabled",
                null
        ));

        integrations.add(integration(
                "GDELT (shipping news)",
                true,
                "Public endpoint, no key required — polling every 15 minutes",
                null
        ));

        integrations.add(integration(
                "Open-Meteo (weather)",
                true,
                "Public endpoint, no key required",
                null
        ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("integrations", integrations);
        return result;
    }

    private Map<String, Object> integration(String name, boolean live, String detail, String lastFetchedAt) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("live", live);
        entry.put("detail", detail);
        entry.put("lastFetchedAt", lastFetchedAt);
        return entry;
    }
}
