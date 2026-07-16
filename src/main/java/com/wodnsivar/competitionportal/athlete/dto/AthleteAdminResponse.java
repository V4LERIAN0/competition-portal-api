package com.wodnsivar.competitionportal.athlete.dto;

import com.wodnsivar.competitionportal.enums.AthleteStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AthleteAdminResponse(
        Long id,
        Long competitionId,
        String competitionName,
        Long categoryId,
        String categoryName,
        Long userAccountId,
        String fullName,
        String email,
        String phoneNumber,
        String country,
        String gymName,
        Integer age,
        LocalDate birthdate,
        BigDecimal height,
        BigDecimal weight,
        String profilePhotoUrl,
        String bibNumber,
        AthleteStatus status,
        Boolean checkedIn,
        String publicBio,
        Instant createdAt,
        Instant updatedAt
) {
}