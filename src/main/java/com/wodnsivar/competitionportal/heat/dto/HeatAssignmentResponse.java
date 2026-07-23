package com.wodnsivar.competitionportal.heat.dto;

import com.wodnsivar.competitionportal.enums.CheckInStatus;

import java.time.LocalDateTime;

public record HeatAssignmentResponse(
        Long id,
        Long athleteId,
        String athleteName,
        String bibNumber,
        Long categoryId,
        String categoryName,
        Integer laneNumber,
        Integer stationNumber,
        CheckInStatus checkInStatus,
        LocalDateTime checkInTime
) {}
