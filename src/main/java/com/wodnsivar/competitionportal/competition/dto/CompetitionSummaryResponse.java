package com.wodnsivar.competitionportal.competition.dto;

import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.RegistrationStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record CompetitionSummaryResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String locationName,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        RegistrationStatus registrationStatus,
        VisibilityStatus visibilityStatus,
        CompetitionStatus status,
        String logoImageUrl,
        String bannerImageUrl
) {
}