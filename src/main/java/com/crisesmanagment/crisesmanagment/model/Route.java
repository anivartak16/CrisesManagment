package com.crisesmanagment.crisesmanagment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g. "Strait of Hormuz"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_supplier_id", nullable = false)
    private Supplier originSupplier;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "base_shipping_cost", nullable = false)
    private Double baseShippingCost;

    @Column(name = "base_risk_score", nullable = false)
    private Double baseRiskScore;

    @Column(name = "origin_lat")
    private Double originLat;

    @Column(name = "origin_lng")
    private Double originLng;
}