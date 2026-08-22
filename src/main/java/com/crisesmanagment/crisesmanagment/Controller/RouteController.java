package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.dto.RouteResponseDto;
import com.crisesmanagment.crisesmanagment.exception.ResourceNotFoundException;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.crisesmanagment.crisesmanagment.service.RouteRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteRepository routeRepository;
    private final RiskEventRepository riskEventRepository;
    private final RouteRiskService routeRiskService;

    @GetMapping
    public List<RouteResponseDto> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public RouteResponseDto getRouteById(@PathVariable Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));
        return toDto(route);
    }

    // NEW: powers a "checked HH:MM:SS · N of M routes currently disrupted"
    // banner on the Routes page, same pattern as the Weather Impact page,
    // so it's visually obvious these risk scores are live-recomputed and
    // not a static seed value.
    @GetMapping("/risk-status")
    public Map<String, Object> getRiskStatus() {
        List<Route> routes = routeRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long disruptedCount = routes.stream()
                .filter(r -> hasActiveEvent(r.getId(), now))
                .count();

        LocalDateTime lastSweep = routeRiskService.getLastSweepAt();

        Map<String, Object> status = new HashMap<>();
        status.put("lastCheckedAt", lastSweep != null ? lastSweep.toString() : null);
        status.put("totalRoutes", routes.size());
        status.put("disruptedRoutes", disruptedCount);
        return status;
    }

    private boolean hasActiveEvent(Long routeId, LocalDateTime now) {
        return riskEventRepository.findByRouteId(routeId).stream()
                .anyMatch(e -> {
                    int durationDays = e.getDurationDays() != null ? e.getDurationDays() : 3;
                    return now.isBefore(e.getCreatedAt().plusDays(durationDays));
                });
    }

    // Builds the "why is this route's risk what it is" context: counts
    // active (non-expired) events on the route and surfaces the most
    // severe one so the UI can explain the number instead of just
    // displaying it.
    private RouteResponseDto toDto(Route route) {
        LocalDateTime now = LocalDateTime.now();

        List<RiskEvent> activeEvents = riskEventRepository.findByRouteId(route.getId()).stream()
                .filter(e -> {
                    int durationDays = e.getDurationDays() != null ? e.getDurationDays() : 3;
                    return now.isBefore(e.getCreatedAt().plusDays(durationDays));
                })
                .toList();

        RiskEvent topEvent = activeEvents.stream()
                .max((a, b) -> Integer.compare(a.getSeverity(), b.getSeverity()))
                .orElse(null);

        return RouteResponseDto.builder()
                .id(route.getId())
                .name(route.getName())
                .originSupplierName(route.getOriginSupplier() != null ? route.getOriginSupplier().getName() : null)
                .originCountry(route.getOriginSupplier() != null ? route.getOriginSupplier().getCountry() : null)
                .originLat(route.getOriginLat())
                .originLng(route.getOriginLng())
                .distanceKm(route.getDistanceKm())
                .baseShippingCost(route.getBaseShippingCost())
                .baseRiskScore(route.getBaseRiskScore())
                .activeEventCount(activeEvents.size())
                .topEventType(topEvent != null ? topEvent.getEventType() : null)
                .topEventSeverity(topEvent != null ? topEvent.getSeverity() : null)
                .build();
    }
}