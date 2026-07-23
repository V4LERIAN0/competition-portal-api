package com.wodnsivar.competitionportal.heat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HeatAssignmentRequest(
        @NotNull Long athleteId,
        @NotNull @Min(1) Integer positionNumber,
        Boolean allowCapacityOverride
) {}
