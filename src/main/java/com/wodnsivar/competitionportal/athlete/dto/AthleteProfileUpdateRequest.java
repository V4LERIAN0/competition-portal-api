package com.wodnsivar.competitionportal.athlete.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AthleteProfileUpdateRequest(

        @Size(max = 80, message = "Country must be 80 characters or less")
        String country,

        @Size(max = 150, message = "Gym name must be 150 characters or less")
        String gymName,

        BigDecimal height,

        BigDecimal weight,

        String profilePhotoUrl,

        String publicBio
) {
}