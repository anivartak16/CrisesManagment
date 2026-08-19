package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

        String responseBody;
        try {
            responseBody = geminiWebClient.post()
                    .uri("/v1beta/models/gemini-2.5-flash:generateContent")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
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