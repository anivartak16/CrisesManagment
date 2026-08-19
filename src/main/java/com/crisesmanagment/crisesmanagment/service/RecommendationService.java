package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RecommendationDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {
    public List<RecommendationDto> getRecommendationsForScenario(Long scenarioId) {
        // minimal stub: return an empty list or one dummy recommendation
        List<RecommendationDto> list = new ArrayList<>();
        RecommendationDto r = new RecommendationDto();
        r.setAction("Contact alternate supplier");
        r.setReason("Stub recommendation for scenario " + scenarioId);
        list.add(r);
        return list;
    }
}
