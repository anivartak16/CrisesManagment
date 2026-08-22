package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventSummaryDto;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only queries over RiskEvent for the activity feed (all routes) and a
 * single route's timeline. Kept separate from GeminiExtractionService, which
 * owns writes/extraction, so this stays a simple, side-effect-free reader.
 */
@Service
@RequiredArgsConstructor
public class RiskEventQueryService {

    private final RiskEventRepository riskEventRepository;

    public List<RiskEventSummaryDto> getAllEvents() {
        return riskEventRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<RiskEventSummaryDto> getEventsForRoute(Long routeId) {
        return riskEventRepository.findByRouteIdOrderByCreatedAtDesc(routeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private RiskEventSummaryDto toDto(RiskEvent event) {
        return new RiskEventSummaryDto(
                event.getId(),
                event.getSource(),
                event.getEventType(),
                event.getSeverity(),
                event.getDurationDays(),
                event.getRawText(),
                event.getRoute() != null ? event.getRoute().getId() : null,
                event.getRoute() != null ? event.getRoute().getName() : null,
                event.getCreatedAt()
        );
    }
}
