package com.crisesmanagment.crisesmanagment.Controller;

import com.crisesmanagment.crisesmanagment.dto.RouteWeatherDto;
import com.crisesmanagment.crisesmanagment.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    // Live weather-risk snapshot for every route — which corridors are
    // currently affected by weather (wind/storms) right now.
    @GetMapping("/routes")
    public List<RouteWeatherDto> getRouteWeatherRisks() {
        return weatherService.getRouteWeatherRisks();
    }
}
