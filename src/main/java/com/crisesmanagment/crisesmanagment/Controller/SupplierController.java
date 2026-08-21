package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.model.Supplier;
import com.crisesmanagment.crisesmanagment.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import com.crisesmanagment.crisesmanagment.exception.ResourceNotFoundException;
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
    private final MarketDataService marketDataService;

    @GetMapping
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @GetMapping("/{id}")
    public Supplier getSupplierById(@PathVariable Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
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
}