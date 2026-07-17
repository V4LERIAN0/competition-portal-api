package com.wodnsivar.competitionportal.event.dto;

import com.wodnsivar.competitionportal.enums.EventStatus;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.enums.WeightUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventCreateRequest(

        @NotBlank(message = "Event code is required")
        @Size(max = 30, message = "Event code must be 30 characters or less")
        String eventCode,

        @NotBlank(message = "Event name is required")
        @Size(max = 180, message = "Event name must be 180 characters or less")
        String name,

        String description,

        String workoutInstructions,

        String movementStandards,

        @NotNull(message = "Score type is required")
        ScoreType scoreType,

        @NotNull(message = "Ranking direction is required")
        RankingDirection rankingDirection,

        Integer timeCapSeconds,

        Integer totalReps,

        Integer repsPerRound,

        Boolean cappedScoringEnabled,

        WeightUnit weightUnit,

        TiebreakType tiebreakType,

        @Size(max = 180, message = "Tiebreak label must be 180 characters or less")
        String tiebreakLabel,

        String tiebreakInstructions,

        RankingDirection tiebreakRankingDirection,

        WeightUnit tiebreakWeightUnit,

        Boolean tiebreakRequired,

        Integer displayOrder,

        Boolean publicVisible,

        Boolean scoreVisible,

        EventStatus status
) {
}
