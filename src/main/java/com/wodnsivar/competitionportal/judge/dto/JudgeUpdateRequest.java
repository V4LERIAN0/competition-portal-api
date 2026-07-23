package com.wodnsivar.competitionportal.judge.dto;
import jakarta.validation.constraints.*;
public record JudgeUpdateRequest(
        @NotBlank @Size(max=180) String fullName,
        @NotBlank @Email @Size(max=150) String email,
        @Size(min=8,max=100) String password,
        Boolean active) {}
