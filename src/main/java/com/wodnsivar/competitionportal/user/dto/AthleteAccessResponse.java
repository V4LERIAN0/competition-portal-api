package com.wodnsivar.competitionportal.user.dto;

public record AthleteAccessResponse(
    Long athleteId,
    String fullName,
    String categoryName,
    String username,
    boolean configured,
    boolean mustChangePassword,
    boolean enabled) {}
