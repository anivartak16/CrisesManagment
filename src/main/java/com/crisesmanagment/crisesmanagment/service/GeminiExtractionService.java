package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventRequestDto;
import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class GeminiExtractionService {

    private final WebClient geminiWebClient;
    private final RiskEventRepository riskEventRepository;
    private final RouteRepository routeRepository;

    public GeminiExtractionService(@Qualifier("geminiWebClient") WebClient geminiWebClient,
                                   RiskEventRepository riskEventRepository,
                                   RouteRepository routeRepository) {
        this.geminiWebClient = geminiWebClient;
        this.riskEventRepository = riskEventRepository;
        this.routeRepository = routeRepository;
    }

    /**
     * Calls Gemini to extract a structured read of the raw text (stored for
     * audit/debug), and persists a RiskEvent. Until Gemini's response is
     * parsed into structured fields (TODO below), severity/route/eventType
     * come from the request's manual-override fields so downstream scenario
     * simulation + optimization can be exercised end-to-end today.
     */
    public RiskEventResponseDto extractAndSave(RiskEventRequestDto request) {
        String rawText = request.getRawText();

        // Gemini generateContent expects this exact shape
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", rawText)))
                )
        );

        // Built as an absolute URI object (not a template string) so Spring's
        // UriComponentsBuilder can't re-encode the ':' in "gemini-2.5-flash:generateContent"
        // into %3A, which Google's API does not recognise (causes a 404).
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
            // getResponseBodyAsString() has Google's real error message —
            // e.getMessage() alone only gives the generic "404 Not Found from POST ..." line.
            System.out.println("GEMINI ERROR STATUS: " + e.getStatusCode());
            System.out.println("GEMINI ERROR BODY: " + e.getResponseBodyAsString());
            responseBody = "{\"error\": \"" + e.getStatusCode() + " - " + e.getResponseBodyAsString().replace("\"", "'") + "\"}";
        } catch (Exception e) {
            responseBody = "{\"error\": \"" + e.getMessage() + "\"}";
        }

        // TODO: replace this with parsing responseBody (force Gemini to return strict JSON
        // with {eventType, severity, durationDays, routeName}) instead of relying on manual overrides.
        Route route = request.getRouteId() != null
                ? routeRepository.findById(request.getRouteId()).orElse(null)
                : null;

        RiskEvent event = RiskEvent.builder()
                .source("gemini")
                .route(route)
                .eventType(request.getEventType() != null ? request.getEventType() : "UNCLASSIFIED")
                .severity(request.getSeverity() != null ? request.getSeverity() : 0)
                .durationDays(request.getDurationDays())
                .rawText(rawText)
                .extractedJson(responseBody)
                .build();
        RiskEvent saved = riskEventRepository.save(event);

        RiskEventResponseDto dto = new RiskEventResponseDto();
        dto.setId(saved.getId());
        dto.setExtractedJson(responseBody == null ? "" : responseBody);
        return dto;
    }
}
