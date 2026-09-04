package com.wodnsivar.competitionportal.event.dto;

import com.wodnsivar.competitionportal.enums.GenderClassification;

public record EventCategoryConfigResponse(
        Long id,
        Long categoryId,
        String categoryName,
        GenderClassification genderClassification,
        String divisionLabel,
        Integer categoryDisplayOrder,
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
