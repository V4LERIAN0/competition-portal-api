package com.wodnsivar.competitionportal.announcement.dto;

import com.wodnsivar.competitionportal.announcement.entity.CompetitionAnnouncement.Audience;
import jakarta.validation.constraints.*;

public record AnnouncementCreateRequest(
    @NotBlank @Size(max = 140) String title,
    @NotBlank @Size(max = 2000) String message,
    @NotNull Audience audience,
    boolean published) {}
