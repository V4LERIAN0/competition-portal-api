package com.wodnsivar.competitionportal.score.service;

import com.wodnsivar.competitionportal.common.exception.BadRequestException;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.service.EffectiveEventConfiguration;
import com.wodnsivar.competitionportal.score.dto.ScoreEntryRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ScoreValidationService {
    public void validate(CompetitionEvent event, ScoreEntryRequest request) {
        validate(event, new EffectiveEventConfiguration(
                event.getTimeCapSeconds(),
                event.getTotalReps(),
                event.getRepsPerRound(),
                event.getCappedScoringEnabled()
        ), request);
    }

    public void validate(
            CompetitionEvent event,
            EffectiveEventConfiguration configuration,
            ScoreEntryRequest request
    ) {
        requireOnlyConfiguredPrimaryValue(event.getScoreType(), request);

        switch (event.getScoreType()) {
            case FOR_TIME -> validateForTime(configuration, request);
            case AMRAP_REPS, EMOM_REPS, ROUNDS_COMPLETED -> require(request.reps(), "Reps are required.");
            case MAX_WEIGHT -> require(request.weightValue(), "Weight is required.");
            case POINTS -> require(request.pointsValue(), "Points are required.");
            case CUSTOM -> require(request.customValue(), "Custom numeric value is required.");
        }

        TiebreakType type = event.getTiebreakType() == null
                ? TiebreakType.NONE
                : event.getTiebreakType();

        if (type == TiebreakType.NONE && request.tiebreakValue() != null) {
            throw new BadRequestException(
                    "This event does not have a configured tiebreak."
            );
        }

        if (Boolean.TRUE.equals(event.getTiebreakRequired())
                && request.tiebreakValue() == null) {
            throw new BadRequestException(
                    "Tiebreak value is required for this event."
            );
        }

        if (type == TiebreakType.TIME && request.tiebreakValue() != null) {
            if (request.tiebreakValue().stripTrailingZeros().scale() > 0) {
                throw new BadRequestException(
                        "TIME tiebreaks must be entered as whole seconds."
                );
            }

            if (configuration.timeCapSeconds() != null
                    && request.tiebreakValue().compareTo(
                    BigDecimal.valueOf(configuration.timeCapSeconds())
            ) > 0) {
                throw new BadRequestException(
                        "Tiebreak time cannot exceed the event time cap."
                );
            }
        }
    }

    private void validateForTime(EffectiveEventConfiguration configuration, ScoreEntryRequest request) {
        if (request.completed() == null) {
            throw new BadRequestException("FOR_TIME scores must indicate whether the athlete completed the workout.");
        }
        if (Boolean.TRUE.equals(request.completed())) {
            require(request.scoreSeconds(), "Completion time is required.");
            if (configuration.timeCapSeconds() != null
                    && request.scoreSeconds() > configuration.timeCapSeconds()) {
                throw new BadRequestException("Completion time cannot exceed the event time cap.");
            }
        } else {
            if (!Boolean.TRUE.equals(configuration.cappedScoringEnabled())) {
                throw new BadRequestException("Incomplete/DNF rep scoring is not enabled for this event.");
            }
            require(request.reps(), "Completed reps are required for an incomplete/DNF score.");
            if (configuration.totalReps() != null && request.reps() >= configuration.totalReps()) {
                throw new BadRequestException("An incomplete score must have fewer reps than the event total.");
            }
        }
    }

    private void requireOnlyConfiguredPrimaryValue(ScoreType type, ScoreEntryRequest r) {
        if (type != ScoreType.FOR_TIME && (r.completed() != null || r.scoreSeconds() != null)) {
            throw new BadRequestException("Completion and time fields are only valid for FOR_TIME events.");
        }
        if (type != ScoreType.FOR_TIME && type != ScoreType.AMRAP_REPS
                && type != ScoreType.EMOM_REPS && type != ScoreType.ROUNDS_COMPLETED && r.reps() != null) {
            throw new BadRequestException("Reps are not valid for this event's score type.");
        }
        if (type != ScoreType.MAX_WEIGHT && r.weightValue() != null) {
            throw new BadRequestException("Weight is only valid for MAX_WEIGHT events.");
        }
        if (type != ScoreType.POINTS && r.pointsValue() != null) {
            throw new BadRequestException("Points are only valid for POINTS events.");
        }
        if (type != ScoreType.CUSTOM && r.customValue() != null) {
            throw new BadRequestException("Custom value is only valid for CUSTOM events.");
        }
    }

    private void require(Object value, String message) {
        if (value == null) throw new BadRequestException(message);
        if (value instanceof BigDecimal number && number.signum() < 0) throw new BadRequestException(message);
    }
}
