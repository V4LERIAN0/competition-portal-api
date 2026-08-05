package com.wodnsivar.competitionportal.leaderboard.service;

import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.score.entity.CompetitionScore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TieBreakService {

    /**
     * Returns a negative value when {@code left} should rank ahead of {@code right}.
     * A zero result means the athletes remain tied after the configured event tiebreak.
     */
    public int compare(CompetitionEvent event, CompetitionScore left, CompetitionScore right) {
        int primaryComparison = comparePrimary(event, left, right);
        if (primaryComparison != 0) {
            return primaryComparison;
        }
        return compareConfiguredTiebreak(event, left.getTiebreakValue(), right.getTiebreakValue());
    }

    private int comparePrimary(CompetitionEvent event, CompetitionScore left, CompetitionScore right) {
        ScoreType scoreType = event.getScoreType();

        if (scoreType == ScoreType.FOR_TIME) {
            return compareForTime(event, left, right);
        }

        BigDecimal leftValue = primaryValue(scoreType, left);
        BigDecimal rightValue = primaryValue(scoreType, right);
        return compareNumeric(leftValue, rightValue, event.getRankingDirection());
    }

    private int compareForTime(CompetitionEvent event, CompetitionScore left, CompetitionScore right) {
        int completionComparison = Integer.compare(
                completionOrder(left.getCompleted()),
                completionOrder(right.getCompleted())
        );
        if (completionComparison != 0) {
            return completionComparison;
        }

        if (Boolean.TRUE.equals(left.getCompleted())) {
            return compareNumeric(
                    decimal(left.getScoreSeconds()),
                    decimal(right.getScoreSeconds()),
                    RankingDirection.LOWER_IS_BETTER
            );
        }

        if (Boolean.FALSE.equals(left.getCompleted())) {
            return compareNumeric(
                    decimal(left.getReps()),
                    decimal(right.getReps()),
                    RankingDirection.HIGHER_IS_BETTER
            );
        }

        return 0;
    }

    private int compareConfiguredTiebreak(
            CompetitionEvent event,
            BigDecimal leftValue,
            BigDecimal rightValue
    ) {
        TiebreakType type = event.getTiebreakType() == null
                ? TiebreakType.NONE
                : event.getTiebreakType();
        if (type == TiebreakType.NONE) {
            return 0;
        }

        RankingDirection direction = event.getTiebreakRankingDirection();
        if (direction == null) {
            direction = type == TiebreakType.TIME
                    ? RankingDirection.LOWER_IS_BETTER
                    : RankingDirection.HIGHER_IS_BETTER;
        }
        return compareNumeric(leftValue, rightValue, direction);
    }

    private BigDecimal primaryValue(ScoreType scoreType, CompetitionScore score) {
        return switch (scoreType) {
            case AMRAP_REPS, EMOM_REPS, ROUNDS_COMPLETED -> decimal(score.getReps());
            case MAX_WEIGHT -> score.getWeightValue();
            case POINTS -> score.getPointsValue();
            case CUSTOM -> score.getCustomValue();
            case FOR_TIME -> null;
        };
    }

    private int compareNumeric(BigDecimal left, BigDecimal right, RankingDirection direction) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        int comparison = left.compareTo(right);
        return direction == RankingDirection.HIGHER_IS_BETTER ? -comparison : comparison;
    }

    private int completionOrder(Boolean completed) {
        if (Boolean.TRUE.equals(completed)) {
            return 0;
        }
        if (Boolean.FALSE.equals(completed)) {
            return 1;
        }
        return 2;
    }

    private BigDecimal decimal(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
