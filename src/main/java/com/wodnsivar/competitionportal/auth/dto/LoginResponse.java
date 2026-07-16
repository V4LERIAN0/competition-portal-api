package com.wodnsivar.competitionportal.auth.dto;

import com.wodnsivar.competitionportal.enums.UserRole;

public record LoginResponse(
        Long id,
        String email,
        UserRole role
) {
}