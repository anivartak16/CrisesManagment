package com.crisesmanagment.crisesmanagment.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class RiskEventRequestDto {
    @NotBlank
    private String rawText;
}
