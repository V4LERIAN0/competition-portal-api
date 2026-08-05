package com.wodnsivar.competitionportal.leaderboard.dto;

import com.wodnsivar.competitionportal.enums.GenderClassification;
import com.wodnsivar.competitionportal.enums.LeaderboardStatus;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.WeightUnit;

import java.time.Instant;
import java.util.List;

public record OverallLeaderboardResponse(
        Long competitionId,
        String competitionName,
        String competitionSlug,
        LeaderboardStatus status,
        Instant lastUpdatedAt,
        List<EventSummary> events,
        List<CategoryLeaderboard> categories
) {
    public record EventSummary(
            Long eventId,
            String eventCode,
            String eventName,
            ScoreType scoreType,
            WeightUnit weightUnit,
            Integer displayOrder
    ) {
    }

    public record CategoryLeaderboard(
            Long categoryId,
            String categoryName,
            GenderClassification genderClassification,
            String divisionLabel,
            Boolean active,
            List<OverallLeaderboardRow> rows
    ) {
    }
}
