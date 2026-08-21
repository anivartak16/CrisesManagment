package com.crisesmanagment.crisesmanagment.optimization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOption {
    private Long supplierId;
    private String supplierName;
    private Long routeId;
    private String routeName;
    private Double availableBarrels;
    private Double unitCost;
    private Double riskScore;
    private Double capacity;
}
