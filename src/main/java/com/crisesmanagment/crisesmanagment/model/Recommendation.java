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
}