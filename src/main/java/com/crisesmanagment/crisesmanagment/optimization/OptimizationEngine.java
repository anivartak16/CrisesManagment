package com.crisesmanagment.crisesmanagment.optimization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OptimizationEngine {

    public List<AllocationPlan> generatePlans(List<SupplierOption> options, double demandBarrels) {
        if (options == null || options.isEmpty()) {
            return List.of(new AllocationPlan(
                    "No available suppliers",
                    0d,
                    0d,
                    Math.max(demandBarrels, 0d),
                    false,
                    List.of()
            ));
        }

        List<SupplierOption> lowestCost = options.stream()
                .sorted(Comparator.comparingDouble(SupplierOption::getUnitCost)
                        .thenComparingDouble(SupplierOption::getRiskScore))
                .collect(Collectors.toList());

        List<SupplierOption> balanced = options.stream()
                .sorted(Comparator.comparingDouble(option ->
                        option.getUnitCost() * 0.7d + option.getRiskScore() * 0.3d))
                .collect(Collectors.toList());

        List<SupplierOption> lowestRisk = options.stream()
                .sorted(Comparator.comparingDouble(SupplierOption::getRiskScore)
                        .thenComparingDouble(SupplierOption::getUnitCost))
                .collect(Collectors.toList());

        List<AllocationPlan> plans = new ArrayList<>();
        plans.add(buildPlan("Lowest cost", lowestCost, demandBarrels, true));
        plans.add(buildPlan("Balanced risk", balanced, demandBarrels, false));
        plans.add(buildPlan("Lowest risk", lowestRisk, demandBarrels, false));
        return plans;
    }

    private AllocationPlan buildPlan(String planName, List<SupplierOption> orderedOptions, double demandBarrels, boolean optimal) {
        double remaining = Math.max(0d, demandBarrels);
        double totalAllocated = 0d;
        double totalCost = 0d;
        double totalRisk = 0d;
        List<AllocationLine> lines = new ArrayList<>();

        for (SupplierOption option : orderedOptions) {
            if (remaining <= 0d) {
                break;
            }

            double available = option.getAvailableBarrels() != null ? option.getAvailableBarrels() : 0d;
            if (available <= 0d) {
                continue;
            }

            double allocated = Math.min(available, remaining);
            if (allocated <= 0d) {
                continue;
            }

            double pct = demandBarrels > 0d ? (allocated / demandBarrels) * 100d : 0d;
            double cost = allocated * (option.getUnitCost() != null ? option.getUnitCost() : 0d);
            double risk = allocated * (option.getRiskScore() != null ? option.getRiskScore() : 0d) / 100d;

            lines.add(new AllocationLine(
                    option.getSupplierId(),
                    option.getSupplierName(),
                    option.getRouteId(),
                    option.getRouteName(),
                    allocated,
                    pct,
                    cost,
                    risk
            ));

            totalAllocated += allocated;
            totalCost += cost;
            totalRisk += risk;
            remaining -= allocated;
        }

        return new AllocationPlan(
                planName,
                totalCost,
                totalRisk,
                Math.max(0d, demandBarrels - totalAllocated),
                optimal,
                lines
        );
    }
}
