package com.wodnsivar.competitionportal.category.dto;

import com.wodnsivar.competitionportal.enums.GenderClassification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 120, message = "Category name must be 120 characters or less")
        String name,

        @NotNull(message = "Gender classification is required")
        GenderClassification genderClassification,

        @Size(max = 80, message = "Division label must be 80 characters or less")
        String divisionLabel,

        String description,

        Integer displayOrder,

        Boolean active
) {
}