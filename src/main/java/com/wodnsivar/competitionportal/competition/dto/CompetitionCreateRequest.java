package com.wodnsivar.competitionportal.competition.dto;

import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.RegistrationStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CompetitionCreateRequest(

        @NotBlank(message = "Competition name is required")
        @Size(max = 150, message = "Competition name must be 150 characters or less")
        String name,

        @Size(max = 180, message = "Slug must be 180 characters or less")
        String slug,

        @Size(max = 500, message = "Short description must be 500 characters or less")
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

        Integer checkInOpenMinutesBeforeHeat
) {
}