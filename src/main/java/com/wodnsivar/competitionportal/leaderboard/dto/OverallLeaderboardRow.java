package com.wodnsivar.competitionportal.leaderboard.dto;

import java.util.List;

public record OverallLeaderboardRow(
        Integer rank,
        Boolean tied,
        Long athleteId,
        String athleteName,
        String bibNumber,
        String country,
        String gymName,
        Long categoryId,
        String categoryName,
        Integer totalPoints,
        Integer scoredEvents,
        Integer totalEvents,
        Integer eventWins,
        Integer topThreePlacements,
        Integer mostRecentEventPlacement,
        List<EventLeaderboardRow> eventResults
) {
}
