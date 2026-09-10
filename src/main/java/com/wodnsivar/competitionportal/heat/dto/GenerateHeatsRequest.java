package com.wodnsivar.competitionportal.heat.dto;

import com.wodnsivar.competitionportal.enums.HeatSeedingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GenerateHeatsRequest(
        @NotNull HeatSeedingMode seedingMode,
        @NotEmpty List<@Valid HeatCategoryScheduleRequest> categorySchedules,
        @NotNull @Min(1) Integer capacity,
        @Min(1) Integer startingHeatNumber,
        @NotNull @Min(1) Integer minutesBetweenHeats,
        Boolean publicVisible,
        Long randomSeed,
        Long sourceEventId
) {
}
