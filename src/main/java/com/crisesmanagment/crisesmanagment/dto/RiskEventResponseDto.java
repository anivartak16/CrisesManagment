package com.crisesmanagment.crisesmanagment.dto;

import lombok.Data;

@Data
public class RiskEventResponseDto {
    private Long id;
    private String extractedJson; // raw extracted JSON string for now
}
