package com.wodnsivar.competitionportal.leaderboard.dto;

import com.wodnsivar.competitionportal.enums.ScoreStatus;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.enums.WeightUnit;

import java.math.BigDecimal;

public record EventLeaderboardRow(
        Integer rank,
        Integer placementPoints,
        Boolean tied,
        Long eventId,
        Long athleteId,
        String athleteName,
        String bibNumber,
        String country,
        String gymName,
        Long categoryId,
        String categoryName,
        Long scoreId,
        ScoreStatus scoreStatus,
        ScoreType scoreType,
        String scoreDisplay,
        Boolean completed,
        Integer scoreSeconds,
        Integer reps,
        BigDecimal weightValue,
        WeightUnit weightUnit,
        BigDecimal pointsValue,
        BigDecimal customValue,
        String tiebreakDisplay,
        BigDecimal tiebreakValue,
        TiebreakType tiebreakType
) {
}
