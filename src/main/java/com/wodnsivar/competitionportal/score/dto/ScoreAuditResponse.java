package com.wodnsivar.competitionportal.score.dto;

import com.wodnsivar.competitionportal.enums.ScoreAuditAction;
import com.wodnsivar.competitionportal.enums.ScoreStatus;
import com.wodnsivar.competitionportal.enums.UserRole;

import java.time.Instant;

public record ScoreAuditResponse(
        Long id,
        ScoreAuditAction action,
        Long actorUserId,
        UserRole actorRole,
        ScoreStatus previousStatus,
        ScoreStatus newStatus,
        String scoreSnapshot,
        String reason,
        Instant occurredAt
) {}
