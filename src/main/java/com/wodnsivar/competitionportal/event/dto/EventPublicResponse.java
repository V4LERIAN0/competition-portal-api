package com.wodnsivar.competitionportal.event.dto;

import com.wodnsivar.competitionportal.enums.EventStatus;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;

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
        Integer displayOrder,
        Boolean scoreVisible,
        EventStatus status
) {
}