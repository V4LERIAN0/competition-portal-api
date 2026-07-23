package com.wodnsivar.competitionportal.heat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HeatAssignmentRequest(
        @NotNull Long athleteId,
        @NotNull @Min(1) Integer laneNumber,
        @Min(1) Integer stationNumber,
        Boolean allowCapacityOverride
) {}
