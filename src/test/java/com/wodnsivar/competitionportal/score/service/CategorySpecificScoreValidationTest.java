package com.wodnsivar.competitionportal.score.service;

import com.wodnsivar.competitionportal.common.exception.BadRequestException;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.service.EffectiveEventConfiguration;
import com.wodnsivar.competitionportal.score.dto.ScoreEntryRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategorySpecificScoreValidationTest {

    private final ScoreValidationService validation = new ScoreValidationService();

    @Test
    void cappedForTimeUsesTheCategorySpecificRepTotal() {
        CompetitionEvent event = CompetitionEvent.builder()
                .scoreType(ScoreType.FOR_TIME)
                .rankingDirection(RankingDirection.LOWER_IS_BETTER)
                .tiebreakType(TiebreakType.TIME)
                .tiebreakRequired(false)
                .build();
        EffectiveEventConfiguration configuration = new EffectiveEventConfiguration(
                600, 99, null, true);

        assertThatCode(() -> validation.validate(
                event,
                configuration,
                incompleteScore(98)
        )).doesNotThrowAnyException();

        assertThatThrownBy(() -> validation.validate(
                event,
                configuration,
                incompleteScore(99)
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fewer reps than the event total");
    }

    @Test
    void tiebreakTimeUsesTheCategorySpecificTimeCap() {
        CompetitionEvent event = CompetitionEvent.builder()
                .scoreType(ScoreType.FOR_TIME)
                .rankingDirection(RankingDirection.LOWER_IS_BETTER)
                .tiebreakType(TiebreakType.TIME)
                .tiebreakRequired(false)
                .build();
        EffectiveEventConfiguration configuration = new EffectiveEventConfiguration(
                600, 99, null, true);
        ScoreEntryRequest score = new ScoreEntryRequest(
                false, null, 50, null, null, null,
                java.math.BigDecimal.valueOf(601), null);

        assertThatThrownBy(() -> validation.validate(event, configuration, score))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot exceed the event time cap");
    }

    private ScoreEntryRequest incompleteScore(int reps) {
        return new ScoreEntryRequest(
                false, null, reps, null, null, null, null, null);
    }
}
