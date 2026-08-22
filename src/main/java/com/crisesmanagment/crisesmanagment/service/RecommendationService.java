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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Runs the deterministic optimizer for a scenario and persists the ranked
     * plans + their per-supplier allocations — but only the FIRST time this is
     * called for a given scenario. Subsequent calls (e.g. re-opening the
     * scenario console, or the history page loading it again) return the
     * already-persisted plans instead of generating and inserting a fresh
     * set of Recommendation/ProcurementAllocation rows every time. Without
     * this, every GET request duplicated the whole plan set in the database.
     */
    @Transactional
    public List<RecommendationDto> getRecommendationsForScenario(Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with id: " + scenarioId));

        List<Recommendation> existing = recommendationRepository.findByScenario_IdOrderByIdAsc(scenarioId);
        if (!existing.isEmpty()) {
            return existing.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

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

    /**
     * Marks one recommendation as ACCEPTED and, since only one plan should be
     * "the" chosen plan per scenario, flips any other still-PROPOSED plan for
     * the same scenario to REJECTED. Returns the accepted plan.
     */
    @Transactional
    public RecommendationDto acceptRecommendation(Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + recommendationId));

        recommendation.setStatus("ACCEPTED");
        recommendationRepository.save(recommendation);

        List<Recommendation> siblings = recommendationRepository
                .findByScenario_IdOrderByIdAsc(recommendation.getScenario().getId());
        for (Recommendation sibling : siblings) {
            if (!sibling.getId().equals(recommendation.getId())
                    && "PROPOSED".equals(sibling.getStatus())) {
                sibling.setStatus("REJECTED");
                recommendationRepository.save(sibling);
            }
        }

        return toDto(recommendation);
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
                .status("PROPOSED")
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
                savedRecommendation.getStatus(),
                lineDtos
        );
    }

    /**
     * Rebuilds a RecommendationDto for an already-persisted recommendation by
     * parsing its stored allocationJson snapshot, rather than re-running the
     * optimizer (which could produce a different result if suppliers/routes
     * changed since the plan was generated — the stored plan should stay stable).
     */
    private RecommendationDto toDto(Recommendation recommendation) {
        List<RecommendationDto.AllocationLineDto> lineDtos = new ArrayList<>();
        try {
            JsonNode lines = objectMapper.readTree(
                    recommendation.getAllocationJson() != null ? recommendation.getAllocationJson() : "[]"
            );
            for (JsonNode line : lines) {
                lineDtos.add(new RecommendationDto.AllocationLineDto(
                        line.path("supplierId").asLong(),
                        line.path("supplierName").asText(),
                        line.path("routeId").asLong(),
                        line.path("routeName").asText(),
                        line.path("allocatedBarrels").asDouble(),
                        line.path("allocatedPct").asDouble(),
                        line.path("cost").asDouble(),
                        line.path("riskContribution").asDouble()
                ));
            }
        } catch (Exception e) {
            // Fall back to an empty allocation list rather than failing the whole
            // history/accept response if a stored snapshot is somehow malformed.
            lineDtos.clear();
        }

        return new RecommendationDto(
                recommendation.getId(),
                recommendation.getPlanName(),
                recommendation.getTotalCost(),
                recommendation.getTotalRisk(),
                recommendation.getSupplyGap(),
                recommendation.getIsOptimal(),
                recommendation.getStatus(),
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
