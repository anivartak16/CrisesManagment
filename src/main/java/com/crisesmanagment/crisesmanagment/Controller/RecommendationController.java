package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.dto.RecommendationDto;
import com.crisesmanagment.crisesmanagment.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{scenarioId}")
    public List<RecommendationDto> getRecommendations(@PathVariable Long scenarioId) {
        return recommendationService.getRecommendationsForScenario(scenarioId);
    }
}