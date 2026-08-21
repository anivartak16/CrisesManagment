package com.crisesmanagment.crisesmanagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteWeatherDto {
    private Long routeId;
    private String routeName;
    private Double originLat;
    private Double originLng;
    private Double windSpeedKph;
    private Double windGustsKph;
    private Integer weatherCode;
    private String weatherDescription;
    // LOW | MODERATE | HIGH
    private String riskLevel;
    // True when current weather is considered severe enough to disrupt this route
    private boolean disrupted;
    private String error; // populated instead of the fields above if the lookup failed
}
