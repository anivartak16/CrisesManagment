package com.crisesmanagment.crisesmanagment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "suppliers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference/master data: real company + country. Not sourced from a
    // live feed because no public API exposes which named companies are
    // selling crude to whom — that's proprietary trading-desk data.
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    // LIVE market data: overwritten from the EIA Brent spot price every time
    // MarketDataService.refreshOilPrices() runs (on startup + every 10 min).
    // The seed value in data.sql is only ever used as a fallback if
    // EIA_API_KEY is missing/unreachable — see priceSource below.
    @Column(name = "base_cost_per_barrel", nullable = false)
    private Double baseCostPerBarrel;

    @Column(nullable = false)
    private Double capacity;

    // Reference data: a fixed geopolitical/logistics risk classification per
    // supplier, used only as an input to the transparent premium formula in
    // MarketDataService (not itself a live feed).
    @Column(name = "risk_baseline", nullable = false)
    private Double riskBaseline;

    // Provenance flags so the UI/judges can see exactly where the current
    // number came from instead of guessing.
    @Column(name = "price_source")
    private String priceSource; // e.g. "EIA_LIVE_BRENT" or "STATIC_FALLBACK"

    @Column(name = "last_price_update")
    private Instant lastPriceUpdate; // null until a live refresh has succeeded at least once
}