package com.wodnsivar.competitionportal.score.dto;

import com.wodnsivar.competitionportal.enums.*;

import java.math.BigDecimal;
import java.time.Instant;

public record ScoreResponse(
        Long id,
        Long competitionId,
        Long eventId,
        String eventCode,
        String eventName,
        ScoreType scoreType,
        Long athleteId,
        String athleteName,
        String bibNumber,
        Long categoryId,
        String categoryName,
        ScoreStatus status,
        Boolean completed,
        Integer scoreSeconds,
        Integer reps,
        BigDecimal weightValue,
        WeightUnit weightUnit,
        BigDecimal pointsValue,
        BigDecimal customValue,
        BigDecimal tiebreakValue,
        TiebreakType tiebreakType,
        String tiebreakLabel,
        String notes,
        Long createdByUserId,
        UserRole createdByRole,
        Long lastModifiedByUserId,
        UserRole lastModifiedByRole,
        Long validatedByUserId,
        Instant validatedAt,
        Long publishedByUserId,
        Instant publishedAt,
        Long lockedByUserId,
        Instant lockedAt,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {}
