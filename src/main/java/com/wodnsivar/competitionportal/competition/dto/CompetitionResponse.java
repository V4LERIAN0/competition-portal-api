package com.wodnsivar.competitionportal.competition.dto;

import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.RegistrationStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CompetitionResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String fullDescription,
        String locationName,
        String address,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        RegistrationStatus registrationStatus,
        VisibilityStatus visibilityStatus,
        CompetitionStatus status,
        String logoImageUrl,
        String bannerImageUrl,
        Integer checkInOpenMinutesBeforeHeat,
        Instant createdAt,
        Instant updatedAt
) {
}