package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class GeminiExtractionService {

    private final WebClient geminiWebClient;

    public GeminiExtractionService(@Qualifier("geminiWebClient") WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
    }

    /**
     * Calls the Gemini generate endpoint with the raw text and returns the full response as a string in extractedJson.
     * This is a thin integration layer; consider improving request/response shape and error handling later.
     */
    public RiskEventResponseDto extractAndSave(String rawText) {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", rawText);
        // Some Gemini endpoints expect a model-specific wrapper; using raw POST to configured URL so apiUrl controls exact endpoint.

        String responseBody;
        try {
            responseBody = geminiWebClient.post()
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            // on error, return a DTO with error message in extractedJson
            RiskEventResponseDto err = new RiskEventResponseDto();
            err.setId(-1L);
            err.setExtractedJson("{\"error\": \"" + e.getMessage().replaceAll("\"","\\\"") + "\"}");
            return err;
        }

        RiskEventResponseDto dto = new RiskEventResponseDto();
        dto.setId(1L);
        dto.setExtractedJson(responseBody == null ? "" : responseBody);
        return dto;
    }
}
