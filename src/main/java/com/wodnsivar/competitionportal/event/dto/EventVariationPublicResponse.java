package com.wodnsivar.competitionportal.event.dto;

import com.wodnsivar.competitionportal.enums.GenderClassification;

public record EventVariationPublicResponse(
        Long categoryId,
        String categoryName,
        GenderClassification genderClassification,
        String divisionLabel,
        Integer displayOrder,
        String label,
        String description,
        String workoutInstructions,
        String movementStandards,
        Integer timeCapSeconds,
        Integer totalReps,
        Integer repsPerRound,
        Boolean cappedScoringEnabled
) {
}
