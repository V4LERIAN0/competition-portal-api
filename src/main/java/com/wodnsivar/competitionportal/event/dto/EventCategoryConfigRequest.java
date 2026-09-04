package com.wodnsivar.competitionportal.event.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventCategoryConfigRequest(
        @NotNull(message = "Category is required for an event variation")
        Long categoryId,

        @Size(max = 120, message = "Variation label must be 120 characters or less")
        String variantLabel,

        String description,

        String workoutInstructions,

        String movementStandards,

        Integer timeCapSeconds,

        Integer totalReps,

        Integer repsPerRound,

        Boolean cappedScoringEnabled
) {
}
