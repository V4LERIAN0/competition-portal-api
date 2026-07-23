package com.wodnsivar.competitionportal.heat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record GenerateRandomHeatsRequest(
        Long categoryId,
        @NotNull @Min(1) Integer capacity,
        @Min(1) Integer startingHeatNumber,
        LocalDateTime firstHeatTime,
        @Min(1) Integer minutesBetweenHeats,
        Boolean publicVisible,
        Long randomSeed
) {}
