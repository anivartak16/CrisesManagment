package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteRiskService {

    private final RouteRepository routeRepository;
    private final RiskEventRepository riskEventRepository;

    // Preserve each route's original seeded risk/cost so live numbers are
    // always computed from a stable baseline, not from an already-mutated
    // value. Populated from Route.seedRiskScore / Route.seedShippingCost,
    // which data.sql resets on every boot — NOT from Route.baseRiskScore /
    // Route.baseShippingCost, which are the live columns this service
    // itself overwrites (see captureBaselines() below for why that
    // distinction matters).
    private final Map<Long, Double> originalRiskBaseline = new HashMap<>();
    private final Map<Long, Double> originalShippingCost = new HashMap<>();

    @PostConstruct
    public void captureBaselines() {
        // IMPORTANT: baselines must come from seed_risk_score / seed_shipping_cost,
        // never from base_risk_score / base_shipping_cost. The base_* columns are
        // LIVE — recomputeRouteRisk() below overwrites them as events fire — so on
        // app restart they may already hold a previously-boosted value. Capturing
        // "baseline" from base_* would treat that boosted value as the new floor,
        // ratcheting risk further up every restart until every route is pinned at
        // max (this was the root cause of routes always showing 10/10). seed_* is
        // reset to the true reference numbers on every boot by data.sql, so it's
        // safe to always trust.
        for (Route r : routeRepository.findAll()) {
            double seedRisk = r.getSeedRiskScore() != null ? r.getSeedRiskScore() : r.getBaseRiskScore();
            double seedCost = r.getSeedShippingCost() != null ? r.getSeedShippingCost() : r.getBaseShippingCost();
            originalRiskBaseline.put(r.getId(), seedRisk);
            originalShippingCost.put(r.getId(), seedCost);
        }
        // Heal immediately: recompute every route right away so a restart never
        // leaves a stale/inflated live number sitting in base_risk_score for up
        // to 5 minutes until the next scheduled sweep.
        recomputeAllRoutes();
    }

    // Safety-net sweep every 5 min — decays/clears effects of expired events
    // even if no new event triggers a recompute in that window.
    @Scheduled(fixedRate = 300000)
    public void recomputeAllRoutes() {
        for (Route r : routeRepository.findAll()) {
            recomputeRouteRisk(r.getId());
        }
    }

    public void recomputeRouteRisk(Long routeId) {
        Route route = routeRepository.findById(routeId).orElse(null);
        if (route == null) return;

        double baseline = originalRiskBaseline.getOrDefault(routeId, route.getBaseRiskScore());
        double baseCost = originalShippingCost.getOrDefault(routeId, route.getBaseShippingCost());

        List<RiskEvent> events = riskEventRepository.findByRouteId(routeId);
        LocalDateTime now = LocalDateTime.now();

        double activeSeverityBoost = 0.0;
        for (RiskEvent e : events) {
            int durationDays = e.getDurationDays() != null ? e.getDurationDays() : 3; // default window
            LocalDateTime expiresAt = e.getCreatedAt().plusDays(durationDays);
            if (now.isBefore(expiresAt)) {
                // severity is 0-10 in RiskEvent, baseline is 0-1 scale — normalize
                activeSeverityBoost += (e.getSeverity() / 10.0) * 0.3;
            }
        }
        // Cap the *sum* of stacked active events, not just the final risk.
        // Without this, repeated testing (or a busy news day with many
        // low/medium events) permanently pins every route at 10/10 for the
        // full 3-day window, since nothing before this capped the boost
        // itself — only the final 0..1 result was clamped. 0.7 still lets
        // 2-3 genuinely severe simultaneous events push risk well above a
        // single event, without one route's history saturating forever.
        activeSeverityBoost = Math.min(activeSeverityBoost, 0.7);

        double effectiveRisk = Math.min(1.0, baseline + activeSeverityBoost);
        double effectiveCost = baseCost * (1 + effectiveRisk * 0.5); // risk surcharge on shipping

        route.setBaseRiskScore(effectiveRisk);
        route.setBaseShippingCost(effectiveCost);
        routeRepository.save(route);

        log.info("Route '{}' risk updated: {} (active events: {})", route.getName(), effectiveRisk, events.size());
    }
}