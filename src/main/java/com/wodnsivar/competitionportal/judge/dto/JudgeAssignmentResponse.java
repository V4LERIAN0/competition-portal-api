package com.wodnsivar.competitionportal.judge.dto;

import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.enums.WeightUnit;

import java.time.LocalDateTime;

public record JudgeAssignmentResponse(
        Long id,
        Long judgeId,
        String judgeName,
        String judgeEmail,
        Boolean judgeActive,
        Long competitionId,
        Long eventId,
        String eventCode,
        String eventName,
        ScoreType scoreType,
        Integer timeCapSeconds,
        Boolean cappedScoringEnabled,
        WeightUnit weightUnit,
        TiebreakType tiebreakType,
        String tiebreakLabel,
        Boolean tiebreakRequired,
        WeightUnit tiebreakWeightUnit,
        Long heatId,
        String heatName,
        Integer heatNumber,
        LocalDateTime scheduledTime,
        Long heatAssignmentId,
        Long athleteId,
        String athleteName,
        String bibNumber,
        Long categoryId,
        String categoryName,
        Integer positionNumber
) {
}