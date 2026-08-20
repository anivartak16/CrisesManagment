package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
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

    public GeminiExtractionService(@Qualifier("geminiWebClient") WebClient geminiWebClient,
                                   RiskEventRepository riskEventRepository) {
        this.geminiWebClient = geminiWebClient;
        this.riskEventRepository = riskEventRepository;
    }

    public RiskEventResponseDto extractAndSave(String rawText) {
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
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent");

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

            RiskEventResponseDto err = new RiskEventResponseDto();
            err.setId(-1L);
            err.setExtractedJson("{\"error\": \"" + e.getStatusCode() + " - " + e.getResponseBodyAsString().replace("\"", "'") + "\"}");
            return err;
        } catch (Exception e) {
            RiskEventResponseDto err = new RiskEventResponseDto();
            err.setId(-1L);
            err.setExtractedJson("{\"error\": \"" + e.getMessage() + "\"}");
            return err;
        }

        RiskEvent event = RiskEvent.builder()
                .source("gemini")
                .eventType("UNCLASSIFIED") // TODO: parse from Gemini response later
                .severity(0)
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