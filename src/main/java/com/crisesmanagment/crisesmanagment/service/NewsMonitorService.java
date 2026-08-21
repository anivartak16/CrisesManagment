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

import java.time.Duration;
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

                // No need to manually encode.
                // WebClient handles query parameter encoding.
                String query =
                        route.getName() + " oil shipping disruption";


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
                        .timeout(Duration.ofSeconds(60))
                        .block();


                // Handle empty response
                if (response == null || response.isBlank()) {

                    log.warn(
                            "Empty response received from GDELT for route {}",
                            route.getName()
                    );

                    continue;
                }


                JsonNode articles =
                        objectMapper
                                .readTree(response)
                                .path("articles");


                if (!articles.isArray()) {

                    log.warn(
                            "No articles found for route {}",
                            route.getName()
                    );

                    continue;
                }


                for (JsonNode article : articles) {

                    String headline =
                            article.path("title").asText();


                    if (headline.isBlank()) {
                        continue;
                    }


                    RiskEventRequestDto dto =
                            new RiskEventRequestDto();

                    dto.setRawText(headline);
                    dto.setRouteId(route.getId());


                    // Gemini will automatically determine
                    // severity and event type
                    geminiExtractionService.extractAndSave(dto);


                    log.info(
                            "Auto-created risk event for route {} from headline: {}",
                            route.getName(),
                            headline
                    );
                }


            } catch (Exception e) {

                log.error(
                        "News poll failed for route {}",
                        route.getName(),
                        e
                );
            }
        }
    }
}