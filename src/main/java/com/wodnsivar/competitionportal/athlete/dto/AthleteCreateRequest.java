package com.wodnsivar.competitionportal.athlete.dto;

import com.wodnsivar.competitionportal.enums.AthleteStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AthleteCreateRequest(

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "Full name is required")
        @Size(max = 180, message = "Full name must be 180 characters or less")
        String fullName,

        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must be 150 characters or less")
        String email,

        @Size(max = 40, message = "Phone number must be 40 characters or less")
        String phoneNumber,

        @Size(max = 80, message = "Country must be 80 characters or less")
        String country,

        @Size(max = 150, message = "Gym name must be 150 characters or less")
        String gymName,

        Integer age,

        LocalDate birthdate,

        BigDecimal height,

        BigDecimal weight,

        String profilePhotoUrl,

        @Size(max = 30, message = "Bib number must be 30 characters or less")
        String bibNumber,

        AthleteStatus status,

        Boolean checkedIn,

        String publicBio
) {
}