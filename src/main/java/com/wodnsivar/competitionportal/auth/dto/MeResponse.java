package com.wodnsivar.competitionportal.auth.dto;

import com.wodnsivar.competitionportal.enums.UserRole;

public record MeResponse(
        Long id,
        String email,
        UserRole role
) {
}