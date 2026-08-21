package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventRequestDto;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsMonitorService {

    @Qualifier("gdeltWebClient")
    private final WebClient gdeltWebClient;
    private final GeminiExtractionService geminiExtractionService;
    private final RouteRepository routeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedRate = 900000) // every 15 min
    public void pollShippingNews() {
        List<Route> routes = routeRepository.findAll();
        for (Route route : routes) {
            try {
                String query = URLEncoder.encode(
                        route.getName() + " oil shipping disruption", StandardCharsets.UTF_8);

                String response = gdeltWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/doc/doc")
                                .queryParam("query", query)
                                .queryParam("mode", "artlist")
                                .queryParam("maxrecords", 3)
                                .queryParam("timespan", "1d")
                                .queryParam("format", "json")
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode articles = objectMapper.readTree(response).path("articles");
                if (!articles.isArray()) continue;

                for (JsonNode article : articles) {
                    String headline = article.path("title").asText();
                    if (headline.isBlank()) continue;

                    RiskEventRequestDto dto = new RiskEventRequestDto();
                    dto.setRawText(headline);
                    dto.setRouteId(route.getId());
                    // severity/eventType left null — GeminiExtractionService's manual-override
                    // fields stay unset so this clearly reads as an auto-detected event
                    geminiExtractionService.extractAndSave(dto);
                    log.info("Auto-created risk event for route {} from headline: {}",
                            route.getName(), headline);
                }
            } catch (Exception e) {
                log.error("News poll failed for route {}", route.getName(), e);
            }
        }
    }
}