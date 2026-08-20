package com.crisesmanagment.crisesmanagment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scenarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_event_id", nullable = false)
    private RiskEvent triggeredByEvent;

    @Column(name = "affected_routes", columnDefinition = "TEXT")
    private String affectedRoutes; // JSON array stored as string

    @Column(nullable = false)
    private String status; // e.g. "SIMULATED", "PENDING"

    @Column(name = "disrupted_route_id")
    private Long disruptedRouteId; // route knocked out/degraded by the triggering event, if any

    @Column(name = "supply_gap_barrels")
    private Double supplyGapBarrels; // barrels that need to be re-sourced as a result
}