package com.crisesmanagment.crisesmanagment.dto;

import lombok.Data;

@Data
public class ScenarioResponseDto {
    private Long id;
    private String summary;
    private String status;
    private Long disruptedRouteId;
    private String disruptedRouteName;
    private Double supplyGapBarrels;
}
