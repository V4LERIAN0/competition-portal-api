package com.wodnsivar.competitionportal.heat.dto;

import com.wodnsivar.competitionportal.enums.HeatStatus;

import java.time.LocalDateTime;
import java.util.List;

public record HeatResponse(
        Long id,
        Long competitionId,
        Long eventId,
        String eventCode,
        String eventName,
        String name,
        Integer heatNumber,
        LocalDateTime scheduledTime,
        LocalDateTime actualStartTime,
        LocalDateTime actualEndTime,
        HeatStatus status,
        Integer capacity,
        Integer assignedCount,
        String notes,
        Integer displayOrder,
        Boolean publicVisible,
        List<HeatAssignmentResponse> assignments
) {}
