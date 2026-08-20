package com.crisesmanagment.crisesmanagment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {
    private Long id;
    private String planName;
    private Double totalCost;
    private Double totalRisk;
    private Double supplyGap;
    private Boolean isOptimal;
    private List<AllocationLineDto> allocations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationLineDto {
        private Long supplierId;
        private String supplierName;
        private Long routeId;
        private String routeName;
        private Double allocatedBarrels;
        private Double allocatedPct;
        private Double cost;
        private Double riskContribution;
    }
}
