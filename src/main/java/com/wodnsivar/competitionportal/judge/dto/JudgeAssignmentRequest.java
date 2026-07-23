package com.wodnsivar.competitionportal.judge.dto;
import jakarta.validation.constraints.NotNull;
public record JudgeAssignmentRequest(@NotNull Long judgeId) {}
