package com.crisesmanagment.crisesmanagment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight view of a RiskEvent for the activity feed / route timeline —
 * intentionally leaner than RiskEventResponseDto (no extractedJson blob),
 * since a list of these can be dozens of rows long.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskEventSummaryDto {
    private Long id;
    private String source;
    private String eventType;
    private Integer severity;
    private Integer durationDays;
    private String rawText;
    private Long routeId;
    private String routeName;
    private LocalDateTime createdAt;
}
