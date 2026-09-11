package com.wodnsivar.competitionportal.announcement.dto;

import com.wodnsivar.competitionportal.announcement.entity.CompetitionAnnouncement.Audience;
import java.time.Instant;

public record AnnouncementResponse(
    Long id,
    String title,
    String message,
    Audience audience,
    boolean published,
    Instant updatedAt) {}
