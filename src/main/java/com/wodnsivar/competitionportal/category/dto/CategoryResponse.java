package com.wodnsivar.competitionportal.category.dto;

import com.wodnsivar.competitionportal.enums.GenderClassification;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        Long competitionId,
        String competitionName,
        String name,
        GenderClassification genderClassification,
        String divisionLabel,
        String description,
        Integer displayOrder,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}