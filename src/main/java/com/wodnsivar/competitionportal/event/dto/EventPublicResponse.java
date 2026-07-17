package com.wodnsivar.competitionportal.event.dto;

import com.wodnsivar.competitionportal.enums.EventStatus;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.enums.WeightUnit;

public record EventPublicResponse(
        Long id,
        Long competitionId,
        String eventCode,
        String name,
        String description,
        String workoutInstructions,
        String movementStandards,
        ScoreType scoreType,
        RankingDirection rankingDirection,
        Integer timeCapSeconds,
        Integer totalReps,
        Integer repsPerRound,
        Boolean cappedScoringEnabled,
        WeightUnit weightUnit,
        TiebreakType tiebreakType,
        String tiebreakLabel,
        String tiebreakInstructions,
        RankingDirection tiebreakRankingDirection,
        WeightUnit tiebreakWeightUnit,
        Boolean tiebreakRequired,
        Integer displayOrder,
        Boolean scoreVisible,
        EventStatus status
) {
}
