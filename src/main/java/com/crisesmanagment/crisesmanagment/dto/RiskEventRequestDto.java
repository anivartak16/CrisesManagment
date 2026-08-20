package com.crisesmanagment.crisesmanagment.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class RiskEventRequestDto {
    @NotBlank
    private String rawText;

    // Manual overrides so the optimizer can be tested end-to-end before the
    // Gemini response is parsed into structured fields (see GeminiExtractionService TODO).
    // Once structured parsing lands, these become optional operator overrides instead
    // of the primary path.
    private Long routeId;      // route this event targets, if any
    private Integer severity;  // 0-10
    private String eventType;  // e.g. "CLOSURE", "SANCTIONS", "ATTACK"
    private Integer durationDays;
}
