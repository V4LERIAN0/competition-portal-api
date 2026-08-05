package com.wodnsivar.competitionportal.score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScoreActionRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must be 500 characters or less")
        String reason
) {}
