package com.wodnsivar.competitionportal.event.service;

import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.common.exception.ResourceNotFoundException;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.EventStatus;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.enums.WeightUnit;
import com.wodnsivar.competitionportal.event.dto.EventCreateRequest;
import com.wodnsivar.competitionportal.event.dto.EventPublicResponse;
import com.wodnsivar.competitionportal.event.dto.EventResponse;
import com.wodnsivar.competitionportal.event.dto.EventUpdateRequest;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final CompetitionEventRepository eventRepository;
    private final CompetitionRepository competitionRepository;

    public EventResponse createEvent(Long competitionId, EventCreateRequest request) {
        Competition competition = findCompetitionOrThrow(competitionId);

        String normalizedEventCode = normalizeEventCode(request.eventCode());

        validateEventCodeIsUnique(competitionId, normalizedEventCode, null);
        TiebreakType tiebreakType = defaultIfNull(request.tiebreakType(), TiebreakType.NONE);
        validateScoreConfiguration(
                request.scoreType(), request.rankingDirection(), request.timeCapSeconds(),
                request.totalReps(), request.repsPerRound(), request.cappedScoringEnabled(),
                request.weightUnit(), tiebreakType, request.tiebreakLabel(),
                request.tiebreakRankingDirection(), request.tiebreakWeightUnit(), request.tiebreakRequired()
        );

        CompetitionEvent event = CompetitionEvent.builder()
                .competition(competition)
                .eventCode(normalizedEventCode)
                .name(request.name().trim())
                .description(request.description())
                .workoutInstructions(request.workoutInstructions())
                .movementStandards(request.movementStandards())
                .scoreType(request.scoreType())
                .rankingDirection(request.rankingDirection())
                .timeCapSeconds(request.timeCapSeconds())
                .totalReps(request.totalReps())
                .repsPerRound(request.repsPerRound())
                .cappedScoringEnabled(defaultIfNull(request.cappedScoringEnabled(), false))
                .weightUnit(request.weightUnit())
                .tiebreakType(tiebreakType)
                .tiebreakLabel(tiebreakType == TiebreakType.NONE ? null : normalizeNullable(request.tiebreakLabel()))
                .tiebreakInstructions(tiebreakType == TiebreakType.NONE ? null : normalizeNullable(request.tiebreakInstructions()))
                .tiebreakRankingDirection(tiebreakType == TiebreakType.NONE ? null : request.tiebreakRankingDirection())
                .tiebreakWeightUnit(tiebreakType == TiebreakType.WEIGHT ? request.tiebreakWeightUnit() : null)
                .tiebreakRequired(tiebreakType != TiebreakType.NONE && defaultIfNull(request.tiebreakRequired(), false))
                .displayOrder(defaultIfNull(request.displayOrder(), 0))
                .publicVisible(defaultIfNull(request.publicVisible(), false))
                .scoreVisible(defaultIfNull(request.scoreVisible(), false))
                .status(defaultIfNull(request.status(), EventStatus.DRAFT))
                .build();

        CompetitionEvent savedEvent = eventRepository.save(event);

        return toResponse(savedEvent);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByCompetition(Long competitionId) {
        ensureCompetitionExists(competitionId);

        return eventRepository.findByCompetitionIdOrderByDisplayOrderAscEventCodeAsc(competitionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        CompetitionEvent event = findEventOrThrow(eventId);
        return toResponse(event);
    }

    public EventResponse updateEvent(Long eventId, EventUpdateRequest request) {
        CompetitionEvent event = findEventOrThrow(eventId);

        String normalizedEventCode = normalizeEventCode(request.eventCode());
        Long competitionId = event.getCompetition().getId();

        validateEventCodeIsUnique(competitionId, normalizedEventCode, event.getId());
        TiebreakType tiebreakType = defaultIfNull(request.tiebreakType(), TiebreakType.NONE);
        validateScoreConfiguration(
                request.scoreType(), request.rankingDirection(), request.timeCapSeconds(),
                request.totalReps(), request.repsPerRound(), request.cappedScoringEnabled(),
                request.weightUnit(), tiebreakType, request.tiebreakLabel(),
                request.tiebreakRankingDirection(), request.tiebreakWeightUnit(), request.tiebreakRequired()
        );

        event.setEventCode(normalizedEventCode);
        event.setName(request.name().trim());
        event.setDescription(request.description());
        event.setWorkoutInstructions(request.workoutInstructions());
        event.setMovementStandards(request.movementStandards());
        event.setScoreType(request.scoreType());
        event.setRankingDirection(request.rankingDirection());
        event.setTimeCapSeconds(request.timeCapSeconds());
        event.setTotalReps(request.totalReps());
        event.setRepsPerRound(request.repsPerRound());
        event.setCappedScoringEnabled(defaultIfNull(request.cappedScoringEnabled(), false));
        event.setWeightUnit(request.weightUnit());
        event.setTiebreakType(tiebreakType);
        event.setTiebreakLabel(tiebreakType == TiebreakType.NONE ? null : normalizeNullable(request.tiebreakLabel()));
        event.setTiebreakInstructions(tiebreakType == TiebreakType.NONE ? null : normalizeNullable(request.tiebreakInstructions()));
        event.setTiebreakRankingDirection(tiebreakType == TiebreakType.NONE ? null : request.tiebreakRankingDirection());
        event.setTiebreakWeightUnit(tiebreakType == TiebreakType.WEIGHT ? request.tiebreakWeightUnit() : null);
        event.setTiebreakRequired(tiebreakType != TiebreakType.NONE && defaultIfNull(request.tiebreakRequired(), false));
        event.setDisplayOrder(defaultIfNull(request.displayOrder(), event.getDisplayOrder()));
        event.setPublicVisible(defaultIfNull(request.publicVisible(), event.getPublicVisible()));
        event.setScoreVisible(defaultIfNull(request.scoreVisible(), event.getScoreVisible()));
        event.setStatus(defaultIfNull(request.status(), event.getStatus()));

        CompetitionEvent savedEvent = eventRepository.save(event);

        return toResponse(savedEvent);
    }

    public void hideEvent(Long eventId) {
        CompetitionEvent event = findEventOrThrow(eventId);
        event.setPublicVisible(false);
        event.setScoreVisible(false);
        event.setStatus(EventStatus.DRAFT);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<EventPublicResponse> getPublicEventsByCompetitionSlug(String competitionSlug) {
        Competition competition = competitionRepository.findBySlug(competitionSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with slug: " + competitionSlug));

        validateCompetitionIsPublic(competition, competitionSlug);

        return eventRepository
                .findByCompetitionIdAndPublicVisibleTrueOrderByDisplayOrderAscEventCodeAsc(competition.getId())
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    private Competition findCompetitionOrThrow(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with id: " + competitionId));
    }

    private void ensureCompetitionExists(Long competitionId) {
        if (!competitionRepository.existsById(competitionId)) {
            throw new ResourceNotFoundException("Competition not found with id: " + competitionId);
        }
    }

    private CompetitionEvent findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    private void validateEventCodeIsUnique(Long competitionId, String eventCode, Long currentEventId) {
        CompetitionEvent existingEvent = eventRepository
                .findByCompetitionIdAndEventCodeIgnoreCase(competitionId, eventCode)
                .orElse(null);

        if (existingEvent == null) {
            return;
        }

        if (currentEventId != null && existingEvent.getId().equals(currentEventId)) {
            return;
        }

        throw new ConflictException("Event code '" + eventCode + "' already exists in this competition.");
    }

    private void validateScoreConfiguration(
            ScoreType scoreType,
            RankingDirection rankingDirection,
            Integer timeCapSeconds,
            Integer totalReps,
            Integer repsPerRound,
            Boolean cappedScoringEnabled,
            WeightUnit weightUnit,
            TiebreakType tiebreakType,
            String tiebreakLabel,
            RankingDirection tiebreakRankingDirection,
            WeightUnit tiebreakWeightUnit,
            Boolean tiebreakRequired
    ) {
        if (scoreType == ScoreType.FOR_TIME && rankingDirection != RankingDirection.LOWER_IS_BETTER) {
            throw new ConflictException("FOR_TIME events should use LOWER_IS_BETTER ranking direction.");
        }

        if ((scoreType == ScoreType.AMRAP_REPS ||
                scoreType == ScoreType.MAX_WEIGHT ||
                scoreType == ScoreType.EMOM_REPS ||
                scoreType == ScoreType.ROUNDS_COMPLETED ||
                scoreType == ScoreType.POINTS) &&
                rankingDirection != RankingDirection.HIGHER_IS_BETTER) {
            throw new ConflictException(scoreType + " events should use HIGHER_IS_BETTER ranking direction.");
        }

        if (timeCapSeconds != null && timeCapSeconds <= 0) {
            throw new ConflictException("Time cap must be greater than zero seconds.");
        }

        if (totalReps != null && totalReps <= 0) {
            throw new ConflictException("Total reps must be greater than zero.");
        }

        if (repsPerRound != null && repsPerRound <= 0) {
            throw new ConflictException("Reps per round must be greater than zero.");
        }

        if (repsPerRound != null && scoreType != ScoreType.AMRAP_REPS && scoreType != ScoreType.EMOM_REPS) {
            throw new ConflictException("Reps per round is only supported for AMRAP_REPS and EMOM_REPS events.");
        }

        if (Boolean.TRUE.equals(cappedScoringEnabled)) {
            if (scoreType != ScoreType.FOR_TIME) {
                throw new ConflictException("Capped scoring is only supported for FOR_TIME events.");
            }
            if (timeCapSeconds == null || totalReps == null) {
                throw new ConflictException("Capped FOR_TIME events require both a time cap and total reps.");
            }
        }

        if (scoreType == ScoreType.MAX_WEIGHT && weightUnit == null) {
            throw new ConflictException("MAX_WEIGHT events require a weight unit.");
        }

        if (scoreType != ScoreType.MAX_WEIGHT && weightUnit != null) {
            throw new ConflictException("A primary weight unit is only valid for MAX_WEIGHT events.");
        }

        validateTiebreakConfiguration(
                tiebreakType, tiebreakLabel, tiebreakRankingDirection,
                tiebreakWeightUnit, tiebreakRequired
        );
    }

    private void validateTiebreakConfiguration(
            TiebreakType tiebreakType,
            String label,
            RankingDirection direction,
            WeightUnit weightUnit,
            Boolean required
    ) {
        if (tiebreakType == TiebreakType.NONE) {
            if (Boolean.TRUE.equals(required)) {
                throw new ConflictException("A tiebreak cannot be required when its type is NONE.");
            }
            return;
        }

        if (label == null || label.isBlank()) {
            throw new ConflictException("Configured tiebreaks require a clear label.");
        }

        if (direction == null) {
            throw new ConflictException("Configured tiebreaks require a ranking direction.");
        }

        if (tiebreakType == TiebreakType.TIME && direction != RankingDirection.LOWER_IS_BETTER) {
            throw new ConflictException("TIME tiebreaks must use LOWER_IS_BETTER ranking direction.");
        }

        if ((tiebreakType == TiebreakType.REPS || tiebreakType == TiebreakType.WEIGHT || tiebreakType == TiebreakType.POINTS)
                && direction != RankingDirection.HIGHER_IS_BETTER) {
            throw new ConflictException(tiebreakType + " tiebreaks must use HIGHER_IS_BETTER ranking direction.");
        }

        if (tiebreakType == TiebreakType.WEIGHT && weightUnit == null) {
            throw new ConflictException("WEIGHT tiebreaks require a weight unit.");
        }

        if (tiebreakType != TiebreakType.WEIGHT && weightUnit != null) {
            throw new ConflictException("A tiebreak weight unit is only valid for WEIGHT tiebreaks.");
        }
    }

    private void validateCompetitionIsPublic(Competition competition, String slug) {
        boolean isPublic = competition.getVisibilityStatus() == VisibilityStatus.PUBLIC;
        boolean isNotHiddenStatus = competition.getStatus() != CompetitionStatus.DRAFT
                && competition.getStatus() != CompetitionStatus.ARCHIVED;

        if (!isPublic || !isNotHiddenStatus) {
            throw new ResourceNotFoundException("Competition not publicly available with slug: " + slug);
        }
    }

    private String normalizeEventCode(String eventCode) {
        return eventCode.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private EventResponse toResponse(CompetitionEvent event) {
        return new EventResponse(
                event.getId(),
                event.getCompetition().getId(),
                event.getCompetition().getName(),
                event.getEventCode(),
                event.getName(),
                event.getDescription(),
                event.getWorkoutInstructions(),
                event.getMovementStandards(),
                event.getScoreType(),
                event.getRankingDirection(),
                event.getTimeCapSeconds(),
                event.getTotalReps(),
                event.getRepsPerRound(),
                event.getCappedScoringEnabled(),
                event.getWeightUnit(),
                event.getTiebreakType(),
                event.getTiebreakLabel(),
                event.getTiebreakInstructions(),
                event.getTiebreakRankingDirection(),
                event.getTiebreakWeightUnit(),
                event.getTiebreakRequired(),
                event.getDisplayOrder(),
                event.getPublicVisible(),
                event.getScoreVisible(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private EventPublicResponse toPublicResponse(CompetitionEvent event) {
        return new EventPublicResponse(
                event.getId(),
                event.getCompetition().getId(),
                event.getEventCode(),
                event.getName(),
                event.getDescription(),
                event.getWorkoutInstructions(),
                event.getMovementStandards(),
                event.getScoreType(),
                event.getRankingDirection(),
                event.getTimeCapSeconds(),
                event.getTotalReps(),
                event.getRepsPerRound(),
                event.getCappedScoringEnabled(),
                event.getWeightUnit(),
                event.getTiebreakType(),
                event.getTiebreakLabel(),
                event.getTiebreakInstructions(),
                event.getTiebreakRankingDirection(),
                event.getTiebreakWeightUnit(),
                event.getTiebreakRequired(),
                event.getDisplayOrder(),
                event.getScoreVisible(),
                event.getStatus()
        );
    }
}
