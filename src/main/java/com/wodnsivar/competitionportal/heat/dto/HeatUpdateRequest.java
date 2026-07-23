package com.wodnsivar.competitionportal.heat.dto;

import com.wodnsivar.competitionportal.enums.HeatStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record HeatUpdateRequest(
        @NotBlank String name,
        @NotNull @Min(1) Integer heatNumber,
        LocalDateTime scheduledTime,
        LocalDateTime actualStartTime,
        LocalDateTime actualEndTime,
        @NotNull HeatStatus status,
        @NotNull @Min(1) Integer capacity,
        String notes,
        @NotNull @Min(0) Integer displayOrder,
        @NotNull Boolean publicVisible,
        Boolean allowCapacityOverride
) {}
