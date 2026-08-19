package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.ScenarioResponseDto;
import org.springframework.stereotype.Service;

@Service
public class ScenarioSimulationService {
    public ScenarioResponseDto simulate(Long eventId) {
        ScenarioResponseDto dto = new ScenarioResponseDto();
        dto.setId(100L + eventId);
        dto.setSummary("Stub simulation for event " + eventId);
        return dto;
    }

    public ScenarioResponseDto getScenarioById(Long id) {
        ScenarioResponseDto dto = new ScenarioResponseDto();
        dto.setId(id);
        dto.setSummary("Stub scenario " + id);
        return dto;
    }
}
