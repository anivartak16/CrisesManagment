package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.ScenarioResponseDto;
import com.crisesmanagment.crisesmanagment.exception.ResourceNotFoundException;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.model.Scenario;
import com.crisesmanagment.crisesmanagment.optimization.RiskAdjustment;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.crisesmanagment.crisesmanagment.repo.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScenarioSimulationService {

    private final RiskEventRepository riskEventRepository;
    private final RouteRepository routeRepository;
    private final ScenarioRepository scenarioRepository;
    private final RiskAdjustment riskAdjustment = new RiskAdjustment();

    /**
     * Turns a RiskEvent into a persisted Scenario: figures out which route it
     * hits and how big the resulting supply gap is. Recommendation generation
     * (the optimizer) reads this Scenario, so all disruption math lives here
     * in exactly one place.
     */
    public ScenarioResponseDto simulate(Long eventId) {
        RiskEvent event = riskEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Risk event not found with id: " + eventId));

        Route disruptedRoute = event.getRoute(); // null if the event wasn't tied to a specific route
        Integer severity = event.getSeverity();

        double supplyGap = riskAdjustment.computeSupplyGap(disruptedRoute, severity);

        Scenario scenario = Scenario.builder()
                .name("Scenario for event #" + eventId + " (" + event.getEventType() + ")")
                .triggeredByEvent(event)
                .affectedRoutes(disruptedRoute != null
                        ? "[\"" + disruptedRoute.getName() + "\"]"
                        : "[]")
                .status("SIMULATED")
                .disruptedRouteId(disruptedRoute != null ? disruptedRoute.getId() : null)
                .supplyGapBarrels(supplyGap)
                .build();

        Scenario saved = scenarioRepository.save(scenario);
        return toDto(saved);
    }

    public ScenarioResponseDto getScenarioById(Long id) {
        Scenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with id: " + id));
        return toDto(scenario);
    }

    /**
     * All scenarios ever simulated, most recent first — backs the History page.
     */
    public List<ScenarioResponseDto> getAllScenarios() {
        return scenarioRepository.findAllByOrderByIdDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ScenarioResponseDto toDto(Scenario scenario) {
        ScenarioResponseDto dto = new ScenarioResponseDto();
        dto.setId(scenario.getId());
        dto.setStatus(scenario.getStatus());
        dto.setDisruptedRouteId(scenario.getDisruptedRouteId());
        dto.setSupplyGapBarrels(scenario.getSupplyGapBarrels());
        String routeName = scenario.getDisruptedRouteId() != null
                ? routeRepository.findById(scenario.getDisruptedRouteId()).map(Route::getName).orElse(null)
                : null;
        dto.setDisruptedRouteName(routeName);
        dto.setSummary(scenario.getSupplyGapBarrels() != null && scenario.getSupplyGapBarrels() > 0
                ? String.format("%.0f barrels/day need re-sourcing due to disruption on %s",
                        scenario.getSupplyGapBarrels(), routeName)
                : "No material supply gap from this event.");
        return dto;
    }
}
