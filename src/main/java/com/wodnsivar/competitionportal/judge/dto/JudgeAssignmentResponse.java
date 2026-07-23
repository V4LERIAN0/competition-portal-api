package com.wodnsivar.competitionportal.judge.dto;
import java.time.LocalDateTime;
public record JudgeAssignmentResponse(
        Long id, Long judgeId, String judgeName, String judgeEmail, Boolean judgeActive,
        Long competitionId, Long eventId, String eventCode, String eventName,
        Long heatId, String heatName, Integer heatNumber, LocalDateTime scheduledTime,
        Long heatAssignmentId, Long athleteId, String athleteName, String bibNumber,
        Long categoryId, String categoryName, Integer positionNumber) {}
