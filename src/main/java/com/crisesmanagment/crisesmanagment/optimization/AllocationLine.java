package com.crisesmanagment.crisesmanagment.optimization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationLine {
    private Long supplierId;
    private String supplierName;
    private Long routeId;
    private String routeName;
    private Double allocatedBarrels;
    private Double allocatedPct;
    private Double cost;
    private Double riskContribution;
}
