package com.crisesmanagment.crisesmanagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDto {

    private Long id;
    private String name;
    private String country;
    private Double baseCostPerBarrel;
    private Double capacity;

    // Static reference classification, unchanged — still seeded from data.sql.
    private Double riskBaseline;

    // NEW: live risk derived from this supplier's route(s) baseRiskScore,
    // which RouteRiskService recomputes in real time off GDELT + Gemini
    // event feeds. Falls back to riskBaseline if the supplier has no
    // routes on file yet (so the UI never shows a blank/zero by accident).
    private Double liveRiskScore;

    // "ROUTE_DERIVED" if liveRiskScore came from real route risk data,
    // "STATIC_FALLBACK" if no routes were found and we fell back to
    // riskBaseline — same provenance-flag pattern as Supplier.priceSource.
    private String riskSource;
}