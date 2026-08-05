package com.wodnsivar.competitionportal.score.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ScoreEntryRequest(
        Boolean completed,
        @Min(value = 0, message = "Score seconds cannot be negative") Integer scoreSeconds,
        @Min(value = 0, message = "Reps cannot be negative") Integer reps,
        @DecimalMin(value = "0.0", message = "Weight cannot be negative") BigDecimal weightValue,
        @DecimalMin(value = "0.0", message = "Points cannot be negative") BigDecimal pointsValue,
        @DecimalMin(value = "0.0", message = "Custom value cannot be negative") BigDecimal customValue,
        @DecimalMin(value = "0.0", message = "Tiebreak value cannot be negative") BigDecimal tiebreakValue,
        @Size(max = 2000, message = "Notes must be 2000 characters or less") String notes
) {}
