package com.crisesmanagment.crisesmanagment.optimization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationPlan {
    private String planName;
    private Double totalCost;
    private Double totalRisk;
    private Double supplyGap;
    private boolean optimal;
    private List<AllocationLine> lines;
}
