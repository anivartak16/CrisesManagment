package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.dto.SupplierResponseDto;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.Supplier;
import com.crisesmanagment.crisesmanagment.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import com.crisesmanagment.crisesmanagment.exception.ResourceNotFoundException;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.crisesmanagment.crisesmanagment.repo.SupplierRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final RouteRepository routeRepository;
    private final MarketDataService marketDataService;

    @GetMapping
    public List<SupplierResponseDto> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public SupplierResponseDto getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        return toDto(supplier);
    }

    // Powers an "Evidence / live data sources" panel in the UI so it's
    // never ambiguous whether pricing is live or fallback data.
    @GetMapping("/market-status")
    public Map<String, Object> getMarketStatus() {
        Double brent = marketDataService.getLastLiveBrentPrice();
        Instant fetchedAt = marketDataService.getLastLiveFetchAt();

        Map<String, Object> status = new HashMap<>();
        status.put("live", brent != null);
        status.put("source", brent != null ? "EIA Europe Brent spot price (RBRTE)" : "Static fallback — set EIA_API_KEY");
        status.put("brentSpotUsdPerBarrel", brent != null ? brent : 0);
        status.put("lastFetchedAt", fetchedAt != null ? fetchedAt.toString() : null); // HashMap allows null values; Map.of() does not
        return status;
    }

    // NEW: derives live risk from this supplier's route(s). RouteRiskService
    // already recomputes route.baseRiskScore in real time off GDELT + Gemini
    // event feeds — this just surfaces that instead of leaving the frontend
    // stuck on the static riskBaseline seed value. Averages across multiple
    // routes if a supplier has more than one; falls back to riskBaseline
    // (flagged via riskSource) if the supplier has no routes on file yet.
    private SupplierResponseDto toDto(Supplier supplier) {
        List<Route> routes = routeRepository.findByOriginSupplierId(supplier.getId());

        double liveRisk;
        String riskSource;

        if (routes.isEmpty()) {
            liveRisk = supplier.getRiskBaseline() != null ? supplier.getRiskBaseline() : 0.0;
            riskSource = "STATIC_FALLBACK";
        } else {
            liveRisk = routes.stream()
                    .mapToDouble(r -> r.getBaseRiskScore() != null ? r.getBaseRiskScore() : 0.0)
                    .average()
                    .orElse(supplier.getRiskBaseline() != null ? supplier.getRiskBaseline() : 0.0);
            riskSource = "ROUTE_DERIVED";
        }

        return SupplierResponseDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .country(supplier.getCountry())
                .baseCostPerBarrel(supplier.getBaseCostPerBarrel())
                .capacity(supplier.getCapacity())
                .riskBaseline(supplier.getRiskBaseline())
                .liveRiskScore(liveRisk)
                .riskSource(riskSource)
                .build();
    }
}