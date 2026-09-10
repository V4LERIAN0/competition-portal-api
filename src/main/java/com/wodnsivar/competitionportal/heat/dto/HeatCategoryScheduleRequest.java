package com.wodnsivar.competitionportal.heat.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record HeatCategoryScheduleRequest(
        @NotNull Long categoryId,
        LocalDateTime firstHeatTime
) {
}
