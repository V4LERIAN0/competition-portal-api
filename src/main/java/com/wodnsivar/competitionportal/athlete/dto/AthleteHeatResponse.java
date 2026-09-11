package com.wodnsivar.competitionportal.athlete.dto;

import com.wodnsivar.competitionportal.enums.*;
import java.time.LocalDateTime;

public record AthleteHeatResponse(
    Long assignmentId,
    Long eventId,
    String eventName,
    String eventCode,
    String heatName,
    int heatNumber,
    int lane,
    LocalDateTime scheduledTime,
    HeatStatus status,
    CheckInStatus checkInStatus,
    LocalDateTime checkInOpensAt,
    boolean canCheckIn) {}
