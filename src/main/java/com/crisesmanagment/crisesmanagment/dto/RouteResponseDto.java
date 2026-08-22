package com.crisesmanagment.crisesmanagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponseDto {

    private Long id;
    private String name;
    private String originSupplierName;
    private String originCountry;
    private Double originLat;  // needed by RouteMap.jsx for pin placement
    private Double originLng;
    private Double distanceKm;
    private Double baseShippingCost;
    private Double baseRiskScore; // live — recomputed by RouteRiskService

    // NEW: context for *why* baseRiskScore is what it is, so the UI can
    // show something more useful than a bare number. Counts only events
    // still inside their duration window (same logic RouteRiskService
    // uses to decide what's "active").
    private Integer activeEventCount;
    private String topEventType;   // event_type of the most severe active event, or null
    private Integer topEventSeverity; // 0-10, or null if no active events
}