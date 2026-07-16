package com.wodnsivar.competitionportal.event.dto;

import com.wodnsivar.competitionportal.enums.EventStatus;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;

import java.time.Instant;

public record EventResponse(
        Long id,
        Long competitionId,
        String competitionName,
        String eventCode,
        String name,
        String description,
        String workoutInstructions,
        String movementStandards,
        ScoreType scoreType,
        RankingDirection rankingDirection,
        Integer timeCapSeconds,
        Integer displayOrder,
        Boolean publicVisible,
        Boolean scoreVisible,
        EventStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}