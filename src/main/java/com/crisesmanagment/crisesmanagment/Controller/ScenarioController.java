package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.dto.ScenarioResponseDto;
import com.crisesmanagment.crisesmanagment.service.ScenarioSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioSimulationService scenarioSimulationService;

    @PostMapping("/{eventId}/simulate")
    public ScenarioResponseDto simulateScenario(@PathVariable Long eventId) {
        return scenarioSimulationService.simulate(eventId);
    }

    @GetMapping("/{id}")
    public ScenarioResponseDto getScenario(@PathVariable Long id) {
        return scenarioSimulationService.getScenarioById(id);
    }

    @GetMapping
    public List<ScenarioResponseDto> getAllScenarios() {
        return scenarioSimulationService.getAllScenarios();
    }
}