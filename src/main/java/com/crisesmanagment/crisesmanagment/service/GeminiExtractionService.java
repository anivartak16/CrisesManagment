package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import org.springframework.stereotype.Service;

@Service
public class GeminiExtractionService {
    // Placeholder stub: returns a simple response containing the input as extractedJson
    public RiskEventResponseDto extractAndSave(String rawText) {
        RiskEventResponseDto dto = new RiskEventResponseDto();
        dto.setId(1L);
        dto.setExtractedJson("{\"rawText\": \"" + rawText.replaceAll("\"","\\\"") + "\"}");
        return dto;
    }
}
