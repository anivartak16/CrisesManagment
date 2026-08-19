package com.crisesmanagment.crisesmanagment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "procurement_allocations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "allocated_pct", nullable = false)
    private Double allocatedPct;

    @Column(nullable = false)
    private Double cost;

    @Column(name = "risk_contribution", nullable = false)
    private Double riskContribution;
}