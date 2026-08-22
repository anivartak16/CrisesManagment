package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.dto.RiskEventRequestDto;
import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import com.crisesmanagment.crisesmanagment.dto.RiskEventSummaryDto;
import com.crisesmanagment.crisesmanagment.service.GeminiExtractionService;
import com.crisesmanagment.crisesmanagment.service.RiskEventQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class RiskEventController {

    private final GeminiExtractionService geminiExtractionService;
    private final RiskEventQueryService riskEventQueryService;

    @PostMapping
    public RiskEventResponseDto createEvent(@Valid @RequestBody RiskEventRequestDto request) {
        return geminiExtractionService.extractAndSave(request);
    }

    @GetMapping
    public List<RiskEventSummaryDto> getAllEvents() {
        return riskEventQueryService.getAllEvents();
    }

    @GetMapping("/route/{routeId}")
    public List<RiskEventSummaryDto> getEventsForRoute(@PathVariable Long routeId) {
        return riskEventQueryService.getEventsForRoute(routeId);
    }
}