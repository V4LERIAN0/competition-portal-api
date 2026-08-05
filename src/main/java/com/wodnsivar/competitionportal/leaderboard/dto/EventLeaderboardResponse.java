package com.wodnsivar.competitionportal.leaderboard.dto;

import com.wodnsivar.competitionportal.enums.GenderClassification;
import com.wodnsivar.competitionportal.enums.LeaderboardStatus;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.WeightUnit;

import java.time.Instant;
import java.util.List;

public record EventLeaderboardResponse(
        Long competitionId,
        String competitionName,
        String competitionSlug,
        Long eventId,
        String eventCode,
        String eventName,
        ScoreType scoreType,
        RankingDirection rankingDirection,
        WeightUnit weightUnit,
        LeaderboardStatus status,
        Instant lastUpdatedAt,
        List<CategoryLeaderboard> categories
) {
    public record CategoryLeaderboard(
            Long categoryId,
            String categoryName,
            GenderClassification genderClassification,
            String divisionLabel,
            Boolean active,
            List<EventLeaderboardRow> rows
    ) {
    }
}
