package com.wodnsivar.competitionportal.leaderboard.dto;

import com.wodnsivar.competitionportal.enums.LeaderboardStatus;

import java.time.Instant;
import java.util.List;

public record AthleteLeaderboardPositionResponse(
        Long competitionId,
        String competitionName,
        Long categoryId,
        String categoryName,
        Long athleteId,
        String athleteName,
        Integer rank,
        Boolean tied,
        Integer totalPoints,
        Integer scoredEvents,
        Integer totalEvents,
        Integer eventWins,
        Integer topThreePlacements,
        LeaderboardStatus status,
        Instant lastUpdatedAt,
        List<EventLeaderboardRow> eventResults
) {
}
