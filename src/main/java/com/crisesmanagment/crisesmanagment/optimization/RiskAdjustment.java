package com.crisesmanagment.crisesmanagment.optimization;

import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.Supplier;

import java.util.ArrayList;
import java.util.List;

public class RiskAdjustment {

    public double computeSupplyGap(Route disruptedRoute, Integer severity) {
        if (disruptedRoute == null || severity == null) {
            return 0d;
        }

        double distance = disruptedRoute.getDistanceKm() != null ? disruptedRoute.getDistanceKm() : 0d;
        double baseRisk = disruptedRoute.getBaseRiskScore() != null ? disruptedRoute.getBaseRiskScore() : 0d;
        double severityMultiplier = 1.0 + (severity / 10.0) * 1.75;
        double demand = (distance * 0.22) + (baseRisk * 3.5) + (severity * 145d);
        return Math.round(demand * severityMultiplier * 10d) / 10d;
    }

    public List<SupplierOption> buildOptions(List<Route> allRoutes, Route disruptedRoute, Integer severity) {
        List<SupplierOption> options = new ArrayList<>();
        if (allRoutes == null) {
            return options;
        }

        for (Route route : allRoutes) {
            if (route == null || route.getOriginSupplier() == null) {
                continue;
            }

            Supplier supplier = route.getOriginSupplier();
            Long supplierId = supplier.getId();
            String supplierName = supplier.getName();
            Long routeId = route.getId();
            String routeName = route.getName();

            double unitCost = (supplier.getBaseCostPerBarrel() != null ? supplier.getBaseCostPerBarrel() : 0d)
                    + (route.getBaseShippingCost() != null ? route.getBaseShippingCost() / 250d : 0d)
                    + (route.getDistanceKm() != null ? route.getDistanceKm() * 0.04d : 0d);

            double riskScore = (supplier.getRiskBaseline() != null ? supplier.getRiskBaseline() : 0d)
                    + (route.getBaseRiskScore() != null ? route.getBaseRiskScore() : 0d);

            double capacity = supplier.getCapacity() != null ? supplier.getCapacity() : 0d;
            if (disruptedRoute != null && routeId != null && routeId.equals(disruptedRoute.getId())) {
                if (severity != null) {
                    riskScore += severity * 7.5d;
                    unitCost += severity * 2.2d;
                    capacity *= Math.max(0.2d, 1.0 - (severity / 12.0d));
                }
            }

            options.add(new SupplierOption(
                    supplierId,
                    supplierName,
                    routeId,
                    routeName,
                    capacity,
                    unitCost,
                    riskScore,
                    capacity
            ));
        }

        return options;
    }
}
