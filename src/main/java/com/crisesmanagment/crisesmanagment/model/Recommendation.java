package com.crisesmanagment.crisesmanagment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "total_cost", nullable = false)
    private Double totalCost;

    @Column(name = "total_risk", nullable = false)
    private Double totalRisk;

    @Column(name = "supply_gap", nullable = false)
    private Double supplyGap;

    @Column(name = "is_optimal", nullable = false)
    private Boolean isOptimal;

    @Column(name = "allocation_json", columnDefinition = "TEXT")
    private String allocationJson;

    /**
     * Lifecycle status for this plan: PROPOSED (default, freshly generated),
     * ACCEPTED (user committed to this plan), or REJECTED (a sibling plan for
     * the same scenario was accepted instead). Nullable-safe default via
     * @Builder.Default so existing rows created before this column existed
     * just read back as null until touched, and new rows always get a value.
     */
    @Builder.Default
    @Column(name = "status")
    private String status = "PROPOSED";
}