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
        validateScoreConfiguration(request.scoreType(), request.rankingDirection(), request.timeCapSeconds());

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
        validateScoreConfiguration(request.scoreType(), request.rankingDirection(), request.timeCapSeconds());

        event.setEventCode(normalizedEventCode);
        event.setName(request.name().trim());
        event.setDescription(request.description());
        event.setWorkoutInstructions(request.workoutInstructions());
        event.setMovementStandards(request.movementStandards());
        event.setScoreType(request.scoreType());
        event.setRankingDirection(request.rankingDirection());
        event.setTimeCapSeconds(request.timeCapSeconds());
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
            Integer timeCapSeconds
    ) {
        if (scoreType == ScoreType.FOR_TIME && rankingDirection != RankingDirection.LOWER_IS_BETTER) {
            throw new ConflictException("FOR_TIME events should use LOWER_IS_BETTER ranking direction.");
        }

        if ((scoreType == ScoreType.AMRAP_REPS ||
                scoreType == ScoreType.MAX_WEIGHT ||
                scoreType == ScoreType.EMOM_REPS ||
                scoreType == ScoreType.POINTS) &&
                rankingDirection != RankingDirection.HIGHER_IS_BETTER) {
            throw new ConflictException(scoreType + " events should use HIGHER_IS_BETTER ranking direction.");
        }

        if (timeCapSeconds != null && timeCapSeconds < 0) {
            throw new ConflictException("Time cap cannot be negative.");
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
                event.getDisplayOrder(),
                event.getScoreVisible(),
                event.getStatus()
        );
    }
}