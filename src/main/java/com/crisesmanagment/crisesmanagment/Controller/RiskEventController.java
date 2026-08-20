package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.dto.RiskEventRequestDto;
import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import com.crisesmanagment.crisesmanagment.service.GeminiExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class RiskEventController {

    private final GeminiExtractionService geminiExtractionService;

    @PostMapping
    public RiskEventResponseDto createEvent(@Valid @RequestBody RiskEventRequestDto request) {
        return geminiExtractionService.extractAndSave(request);
    }
}