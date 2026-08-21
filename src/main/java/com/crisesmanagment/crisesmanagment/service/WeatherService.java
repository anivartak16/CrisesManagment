package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RouteWeatherDto;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Answers "which shipping routes are currently affected by weather?" using
 * Open-Meteo (free, no API key) against each route's origin coordinates.
 * Wind speed/gusts are the proxy for maritime disruption risk (storms,
 * rough seas that force rerouting or slow transit).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    @Qualifier("weatherWebClient")
    private final WebClient weatherWebClient;
    private final RouteRepository routeRepository;
    private final RiskEventRepository riskEventRepository;
    private final RouteRiskService routeRiskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${weather.risk.wind-threshold-kph:45}")
    private double windThresholdKph;

    /**
     * Live weather-risk snapshot for every route on file. Used by the
     * on-demand GET endpoint — always fresh, not cached, since Open-Meteo is
     * fast and free.
     */
    public List<RouteWeatherDto> getRouteWeatherRisks() {
        return routeRepository.findAll().stream()
                .map(this::assessRoute)
                .toList();
    }

    private RouteWeatherDto assessRoute(Route route) {
        if (route.getOriginLat() == null || route.getOriginLng() == null) {
            return RouteWeatherDto.builder()
                    .routeId(route.getId())
                    .routeName(route.getName())
                    .error("Route has no origin coordinates on file")
                    .build();
        }

        try {
            String response = weatherWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", route.getOriginLat())
                            .queryParam("longitude", route.getOriginLng())
                            .queryParam("current", "wind_speed_10m,wind_gusts_10m,weather_code")
                            .queryParam("wind_speed_unit", "kmh")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode current = objectMapper.readTree(response).path("current");
            double windSpeed = current.path("wind_speed_10m").asDouble(0);
            double windGusts = current.path("wind_gusts_10m").asDouble(windSpeed);
            int weatherCode = current.path("weather_code").asInt(0);

            String riskLevel;
            boolean disrupted;
            double worstWind = Math.max(windSpeed, windGusts);
            if (worstWind >= windThresholdKph || isSevereWeatherCode(weatherCode)) {
                riskLevel = "HIGH";
                disrupted = true;
            } else if (worstWind >= windThresholdKph * 0.6) {
                riskLevel = "MODERATE";
                disrupted = false;
            } else {
                riskLevel = "LOW";
                disrupted = false;
            }

            return RouteWeatherDto.builder()
                    .routeId(route.getId())
                    .routeName(route.getName())
                    .originLat(route.getOriginLat())
                    .originLng(route.getOriginLng())
                    .windSpeedKph(windSpeed)
                    .windGustsKph(windGusts)
                    .weatherCode(weatherCode)
                    .weatherDescription(describeWeatherCode(weatherCode))
                    .riskLevel(riskLevel)
                    .disrupted(disrupted)
                    .build();
        } catch (Exception e) {
            log.error("Weather lookup failed for route {}", route.getName(), e);
            return RouteWeatherDto.builder()
                    .routeId(route.getId())
                    .routeName(route.getName())
                    .error("Weather lookup failed: " + e.getMessage())
                    .build();
        }
    }

    // WMO weather interpretation codes: 95-99 = thunderstorm, 82 = violent
    // rain showers, 75 = heavy snow, 67 = heavy freezing rain — all conditions
    // that would realistically disrupt a shipping corridor regardless of wind.
    private boolean isSevereWeatherCode(int code) {
        return code == 82 || code == 75 || code == 67 || (code >= 95 && code <= 99);
    }

    private String describeWeatherCode(int code) {
        if (code == 0) return "Clear sky";
        if (code <= 3) return "Partly cloudy";
        if (code <= 48) return "Fog";
        if (code <= 57) return "Drizzle";
        if (code <= 67) return "Rain";
        if (code <= 77) return "Snow";
        if (code <= 82) return "Rain showers";
        if (code <= 86) return "Snow showers";
        if (code <= 99) return "Thunderstorm";
        return "Unknown";
    }

    // Periodically sweeps all routes and auto-logs a RiskEvent (source
    // "weather") for any route currently flagged HIGH risk, so severe
    // weather shows up in the same pipeline as Gemini/news-derived events —
    // without needing an operator to notice and log it manually.
    @Scheduled(fixedRate = 1800000) // every 30 min
    public void autoLogSevereWeatherEvents() {
        for (RouteWeatherDto w : getRouteWeatherRisks()) {
            if (w.getError() != null || !w.isDisrupted()) continue;
            if (recentWeatherEventExists(w.getRouteId())) continue;

            Route route = routeRepository.findById(w.getRouteId()).orElse(null);
            if (route == null) continue;

            RiskEvent event = RiskEvent.builder()
                    .source("weather")
                    .route(route)
                    .eventType("WEATHER")
                    .severity(6) // HIGH weather risk defaults to a moderate-high severity
                    .durationDays(1)
                    .rawText(String.format(
                            "Auto-detected severe weather on %s: %s, wind %.0f km/h (gusts %.0f km/h)",
                            route.getName(), w.getWeatherDescription(), w.getWindSpeedKph(), w.getWindGustsKph()))
                    .extractedJson("{\"source\":\"open-meteo\",\"riskLevel\":\"HIGH\"}")
                    .build();
            riskEventRepository.save(event);
            routeRiskService.recomputeRouteRisk(route.getId());
            log.info("Auto-logged weather risk event for route '{}': {}", route.getName(), w.getWeatherDescription());
        }
    }

    // Avoid spamming a new event every 30-min sweep while the same storm is
    // still active on a route — only re-log after the previous weather event
    // would have expired (its 1-day duration).
    private boolean recentWeatherEventExists(Long routeId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        return riskEventRepository.findByRouteId(routeId).stream()
                .anyMatch(e -> "weather".equals(e.getSource()) && e.getCreatedAt() != null && e.getCreatedAt().isAfter(cutoff));
    }
}
