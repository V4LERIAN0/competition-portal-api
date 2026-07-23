package com.wodnsivar.competitionportal.judge.dto;

import java.time.Instant;

public record JudgeResponse(
        Long id,
        Long competitionId,
        Long userAccountId,
        String fullName,
        String email,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {}