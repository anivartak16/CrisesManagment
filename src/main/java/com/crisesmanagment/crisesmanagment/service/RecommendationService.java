package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RecommendationDto;
import com.crisesmanagment.crisesmanagment.exception.ResourceNotFoundException;
import com.crisesmanagment.crisesmanagment.model.ProcurementAllocation;
import com.crisesmanagment.crisesmanagment.model.Recommendation;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.Scenario;
import com.crisesmanagment.crisesmanagment.model.Supplier;
import com.crisesmanagment.crisesmanagment.optimization.AllocationLine;
import com.crisesmanagment.crisesmanagment.optimization.AllocationPlan;
import com.crisesmanagment.crisesmanagment.optimization.OptimizationEngine;
import com.crisesmanagment.crisesmanagment.optimization.RiskAdjustment;
import com.crisesmanagment.crisesmanagment.optimization.SupplierOption;
import com.crisesmanagment.crisesmanagment.repo.ProcurementAllocationRepository;
import com.crisesmanagment.crisesmanagment.repo.RecommendationRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.crisesmanagment.crisesmanagment.repo.ScenarioRepository;
import com.crisesmanagment.crisesmanagment.repo.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ScenarioRepository scenarioRepository;
    private final RouteRepository routeRepository;
    private final SupplierRepository supplierRepository;
    private final RecommendationRepository recommendationRepository;
    private final ProcurementAllocationRepository procurementAllocationRepository;

    private final OptimizationEngine optimizationEngine = new OptimizationEngine();
    private final RiskAdjustment riskAdjustment = new RiskAdjustment();

    /**
     * Runs the deterministic optimizer for a scenario and persists the ranked
     * plans + their per-supplier allocations. Safe to call repeatedly for the
     * same scenario (each call produces a fresh set of Recommendation rows) —
     * callers reading history should filter to the latest by createdAt/id if needed.
     */
    @Transactional
    public List<RecommendationDto> getRecommendationsForScenario(Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with id: " + scenarioId));

        List<Route> allRoutes = routeRepository.findAll();
        Route disruptedRoute = scenario.getDisruptedRouteId() != null
                ? routeRepository.findById(scenario.getDisruptedRouteId()).orElse(null)
                : null;
        Integer severity = scenario.getTriggeredByEvent() != null
                ? scenario.getTriggeredByEvent().getSeverity()
                : null;

        List<SupplierOption> options = riskAdjustment.buildOptions(allRoutes, disruptedRoute, severity);
        double demandBarrels = scenario.getSupplyGapBarrels() != null ? scenario.getSupplyGapBarrels() : 0;

        List<AllocationPlan> plans = optimizationEngine.generatePlans(options, demandBarrels);

        return plans.stream()
                .map(plan -> persistPlan(scenario, plan))
                .collect(Collectors.toList());
    }

    private RecommendationDto persistPlan(Scenario scenario, AllocationPlan plan) {
        Recommendation recommendation = Recommendation.builder()
                .scenario(scenario)
                .planName(plan.getPlanName())
                .totalCost(plan.getTotalCost())
                .totalRisk(plan.getTotalRisk())
                .supplyGap(plan.getSupplyGap())
                .isOptimal(plan.isOptimal())
                .allocationJson(toJson(plan))
                .build();
        Recommendation savedRecommendation = recommendationRepository.save(recommendation);

        List<RecommendationDto.AllocationLineDto> lineDtos = plan.getLines().stream()
                .map(line -> persistAllocation(savedRecommendation, line))
                .collect(Collectors.toList());

        return new RecommendationDto(
                savedRecommendation.getId(),
                plan.getPlanName(),
                plan.getTotalCost(),
                plan.getTotalRisk(),
                plan.getSupplyGap(),
                plan.isOptimal(),
                lineDtos
        );
    }

    private RecommendationDto.AllocationLineDto persistAllocation(Recommendation recommendation, AllocationLine line) {
        Supplier supplier = supplierRepository.findById(line.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + line.getSupplierId()));

        ProcurementAllocation allocation = ProcurementAllocation.builder()
                .recommendation(recommendation)
                .supplier(supplier)
                .allocatedPct(line.getAllocatedPct())
                .cost(line.getCost())
                .riskContribution(line.getRiskContribution())
                .build();
        procurementAllocationRepository.save(allocation);

        return new RecommendationDto.AllocationLineDto(
                line.getSupplierId(), line.getSupplierName(),
                line.getRouteId(), line.getRouteName(),
                line.getAllocatedBarrels(), line.getAllocatedPct(),
                line.getCost(), line.getRiskContribution()
        );
    }

    private String toJson(AllocationPlan plan) {
        // Lightweight hand-built JSON (avoids pulling Jackson ObjectMapper in just for this)
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < plan.getLines().size(); i++) {
            AllocationLine l = plan.getLines().get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"supplierId\":%d,\"supplierName\":\"%s\",\"routeId\":%d,\"routeName\":\"%s\"," +
                    "\"allocatedBarrels\":%.2f,\"allocatedPct\":%.2f,\"cost\":%.2f,\"riskContribution\":%.4f}",
                    l.getSupplierId(), l.getSupplierName(), l.getRouteId(), l.getRouteName(),
                    l.getAllocatedBarrels(), l.getAllocatedPct(), l.getCost(), l.getRiskContribution()
            ));
        }
        sb.append("]");
        return sb.toString();
    }
}
