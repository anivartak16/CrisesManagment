package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventRequestDto;
import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiExtractionService {

    private final WebClient geminiWebClient;
    private final RiskEventRepository riskEventRepository;
    private final RouteRepository routeRepository;
    private final RouteRiskService routeRiskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiExtractionService(@Qualifier("geminiWebClient") WebClient geminiWebClient,
                                   RiskEventRepository riskEventRepository,
                                   RouteRepository routeRepository,
                                   RouteRiskService routeRiskService) {
        this.geminiWebClient = geminiWebClient;
        this.riskEventRepository = riskEventRepository;
        this.routeRepository = routeRepository;
        this.routeRiskService = routeRiskService;
    }

    public RiskEventResponseDto extractAndSave(RiskEventRequestDto request) {
        String rawText = request.getRawText();

        // Force Gemini to return ONLY strict JSON — this is what makes severity
        // real instead of always defaulting to 0/manual-override.
        String prompt = "Analyze this crude oil supply-chain news headline and respond with ONLY "
                + "raw JSON, no markdown, no explanation, in exactly this shape: "
                + "{\"severity\": <integer 0-10>, \"eventType\": \"<CLOSURE|SANCTIONS|ATTACK|WEATHER|OTHER>\", "
                + "\"durationDays\": <integer>}. Headline: \"" + rawText + "\"";

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        URI uri = URI.create(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");

        String responseBody;
        try {
            responseBody = geminiWebClient.post()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GEMINI ERROR STATUS: {} BODY: {}", e.getStatusCode(), e.getResponseBodyAsString());
            responseBody = "{\"error\": \"" + e.getStatusCode() + " - " + e.getResponseBodyAsString().replace("\"", "'") + "\"}";
        } catch (Exception e) {
            responseBody = "{\"error\": \"" + e.getMessage() + "\"}";
        }

        Route route = request.getRouteId() != null
                ? routeRepository.findById(request.getRouteId()).orElse(null)
                : null;

        // Try parsing Gemini's structured JSON out of the response envelope.
        // Falls back to manual-override fields (or defaults) if parsing fails —
        // so a bad/unexpected Gemini response never crashes event creation.
        Integer parsedSeverity = null;
        String parsedEventType = null;
        Integer parsedDuration = null;
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String candidateText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text").asText();
            String cleaned = candidateText.replaceAll("```json|```", "").trim();
            JsonNode parsed = objectMapper.readTree(cleaned);
            parsedSeverity = parsed.path("severity").asInt();
            parsedEventType = parsed.path("eventType").asText();
            parsedDuration = parsed.path("durationDays").asInt();
        } catch (Exception e) {
            log.warn("Could not parse structured severity from Gemini response, using overrides/defaults: {}", e.getMessage());
        }

        RiskEvent event = RiskEvent.builder()
                .source("gemini")
                .route(route)
                .eventType(request.getEventType() != null ? request.getEventType()
                        : (parsedEventType != null ? parsedEventType : "UNCLASSIFIED"))
                .severity(request.getSeverity() != null ? request.getSeverity()
                        : (parsedSeverity != null ? parsedSeverity : 0))
                .durationDays(request.getDurationDays() != null ? request.getDurationDays() : parsedDuration)
                .rawText(rawText)
                .extractedJson(responseBody)
                .build();
        RiskEvent saved = riskEventRepository.save(event);

        // Immediately recompute the affected route's live risk/cost — this is
        // what makes the routes page real-time instead of waiting for the poller.
        if (route != null) {
            routeRiskService.recomputeRouteRisk(route.getId());
        }

        RiskEventResponseDto dto = new RiskEventResponseDto();
        dto.setId(saved.getId());
        dto.setExtractedJson(responseBody == null ? "" : responseBody);
        return dto;
    }
}