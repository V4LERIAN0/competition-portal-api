package com.wodnsivar.competitionportal.athlete.dto;

import com.wodnsivar.competitionportal.enums.AthleteStatus;

import java.math.BigDecimal;

public record AthletePublicResponse(
        Long id,
        Long competitionId,
        Long categoryId,
        String categoryName,
        String fullName,
        String country,
        String gymName,
        BigDecimal height,
        BigDecimal weight,
        String profilePhotoUrl,
        String bibNumber,
        AthleteStatus status,
        String publicBio
) {
}