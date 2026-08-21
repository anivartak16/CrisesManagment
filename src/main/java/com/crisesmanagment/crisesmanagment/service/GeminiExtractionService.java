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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

    // The Gemini model to call. Previously this was hardcoded to
    // "gemini-2.5-flash", which Google has since retired (404 NOT_FOUND) —
    // now driven by config so a model bump never requires a code change again.
    @Value("${gemini.api.model:gemini-3.6-flash}")
    private String geminiModel;

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

        // Build the list of known routes so Gemini can pick the one the
        // headline is actually about instead of requiring the operator to
        // tag it by hand every time.
        List<Route> allRoutes = routeRepository.findAll();
        String routeList = allRoutes.isEmpty()
                ? "(no routes on file)"
                : allRoutes.stream().map(Route::getName).reduce((a, b) -> a + ", " + b).orElse("");

        // Force Gemini to return ONLY strict JSON — this is what makes severity
        // real instead of always defaulting to 0/manual-override. Now also asks
        // Gemini to pick the affected route by name so the "Affected route"
        // field can be auto-detected instead of always requiring manual tagging.
        String prompt = "Analyze this crude oil supply-chain news headline and respond with ONLY "
                + "raw JSON, no markdown, no explanation, in exactly this shape: "
                + "{\"severity\": <integer 0-10>, \"eventType\": \"<CLOSURE|SANCTIONS|ATTACK|WEATHER|OTHER>\", "
                + "\"durationDays\": <integer>, \"routeName\": \"<one of: " + routeList + ", or null if none clearly match>\"}. "
                + "Headline: \"" + rawText + "\"";

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        String responseBody;
        try {
            responseBody = geminiWebClient.post()
                    .uri("/v1beta/models/" + geminiModel + ":generateContent")
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

        // Try parsing Gemini's structured JSON out of the response envelope.
        // Falls back to manual-override fields (or defaults) if parsing fails —
        // so a bad/unexpected Gemini response never crashes event creation.
        Integer parsedSeverity = null;
        String parsedEventType = null;
        Integer parsedDuration = null;
        String parsedRouteName = null;
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String candidateText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text").asText();
            String cleaned = candidateText.replaceAll("```json|```", "").trim();
            JsonNode parsed = objectMapper.readTree(cleaned);
            parsedSeverity = parsed.path("severity").asInt();
            parsedEventType = parsed.path("eventType").asText();
            parsedDuration = parsed.path("durationDays").asInt();
            if (parsed.hasNonNull("routeName")) {
                parsedRouteName = parsed.path("routeName").asText();
            }
        } catch (Exception e) {
            log.warn("Could not parse structured severity from Gemini response, using overrides/defaults: {}", e.getMessage());
        }

        // Route resolution priority: explicit routeId from the request wins
        // (manual tagging still overrides), otherwise fall back to whatever
        // route Gemini matched against the headline by name.
        Route route = null;
        boolean autoDetectedRoute = false;
        if (request.getRouteId() != null) {
            route = routeRepository.findById(request.getRouteId()).orElse(null);
        } else if (parsedRouteName != null && !parsedRouteName.isBlank()
                && !"null".equalsIgnoreCase(parsedRouteName.trim())) {
            String needle = parsedRouteName.trim();
            route = allRoutes.stream()
                    .filter(r -> r.getName().equalsIgnoreCase(needle) || r.getName().contains(needle) || needle.contains(r.getName()))
                    .findFirst()
                    .orElse(null);
            autoDetectedRoute = route != null;
        }

        RiskEvent event = RiskEvent.builder()
                .source("gemini")
                .route(route)
                .eventType(request.getEventType() != null ? request.getEventType()
                        : (parsedEventType != null && !parsedEventType.isBlank() ? parsedEventType : "UNCLASSIFIED"))
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
        dto.setSeverity(saved.getSeverity());
        dto.setEventType(saved.getEventType());
        dto.setDurationDays(saved.getDurationDays());
        dto.setRouteId(route != null ? route.getId() : null);
        dto.setRouteName(route != null ? route.getName() : null);
        dto.setAutoDetectedRoute(autoDetectedRoute);
        return dto;
    }
}