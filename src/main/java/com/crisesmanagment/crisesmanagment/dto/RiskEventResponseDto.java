package com.crisesmanagment.crisesmanagment.dto;

import lombok.Data;

@Data
public class RiskEventResponseDto {
    private Long id;
    private String extractedJson; // raw extracted JSON string for now

    // Structured fields so the frontend doesn't have to re-parse extractedJson
    // to show what Gemini (or the manual override) actually decided.
    private Integer severity;
    private String eventType;
    private Integer durationDays;
    private Long routeId;
    private String routeName;
    // True when no routeId was supplied and Gemini matched the headline to a
    // route by name on its own.
    private boolean autoDetectedRoute;
}
