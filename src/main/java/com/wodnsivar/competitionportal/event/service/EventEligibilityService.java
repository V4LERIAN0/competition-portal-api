package com.wodnsivar.competitionportal.event.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.common.exception.BadRequestException;
import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.enums.EventEligibilityMode;
import com.wodnsivar.competitionportal.enums.HeatStatus;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.entity.CompetitionEventParticipant;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventParticipantRepository;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatAthleteRepository;
import com.wodnsivar.competitionportal.score.repository.CompetitionScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EventEligibilityService {

    private final CompetitionEventParticipantRepository participants;
    private final CompetitionAthleteRepository athletes;
    private final CompetitionHeatAthleteRepository heatAssignments;
    private final CompetitionScoreRepository scores;

    @Transactional(readOnly = true)
    public boolean isEligible(CompetitionEvent event, CompetitionAthlete athlete) {
        if (mode(event) == EventEligibilityMode.ALL_ACTIVE) {
            return true;
        }
        return participants.existsByEventIdAndAthleteId(event.getId(), athlete.getId());
    }

    @Transactional(readOnly = true)
    public Set<Long> explicitlyEligibleAthleteIds(CompetitionEvent event) {
        if (mode(event) == EventEligibilityMode.ALL_ACTIVE) {
            return null;
        }
        return new HashSet<>(participants.findAthleteIdsByEventId(event.getId()));
    }

    public void requireEligible(CompetitionEvent event, CompetitionAthlete athlete) {
        if (!isEligible(event, athlete)) {
            throw new BadRequestException("Athlete is not eligible to compete in this event.");
        }
    }

    public void replaceParticipants(CompetitionEvent event, List<Long> athleteIds) {
        if (mode(event) == EventEligibilityMode.ALL_ACTIVE) {
            participants.deleteByEventId(event.getId());
            return;
        }

        List<Long> requestedIds = athleteIds == null ? List.of() : athleteIds;
        Set<Long> uniqueIds = new HashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new BadRequestException("Eligible athlete ids cannot contain duplicates.");
        }

        List<Long> assignedAthleteIds = heatAssignments.findAthleteIdsForEventExcludingHeatStatus(
                event.getId(), HeatStatus.CANCELLED);
        if (!uniqueIds.containsAll(assignedAthleteIds)) {
            throw new ConflictException(
                    "Eligible athletes cannot be removed while they are assigned to an active heat.");
        }
        if (!uniqueIds.containsAll(scores.findAthleteIdsByEventId(event.getId()))) {
            throw new ConflictException(
                    "Eligible athletes cannot be removed after a score has been recorded for them.");
        }

        List<CompetitionAthlete> selectedAthletes = athletes.findAllById(uniqueIds);
        if (selectedAthletes.size() != uniqueIds.size()) {
            throw new BadRequestException("One or more eligible athletes could not be found.");
        }

        for (CompetitionAthlete athlete : selectedAthletes) {
            if (!event.getCompetition().getId().equals(athlete.getCompetition().getId())) {
                throw new BadRequestException("All eligible athletes must belong to the event's competition.");
            }
        }

        participants.deleteByEventId(event.getId());
        participants.flush();
        for (CompetitionAthlete athlete : selectedAthletes) {
            participants.save(CompetitionEventParticipant.builder()
                    .event(event)
                    .athlete(athlete)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<Long> participantIds(CompetitionEvent event) {
        if (mode(event) == EventEligibilityMode.ALL_ACTIVE) {
            return List.of();
        }
        return participants.findAthleteIdsByEventId(event.getId());
    }

    private EventEligibilityMode mode(CompetitionEvent event) {
        return event.getEligibilityMode() == null
                ? EventEligibilityMode.ALL_ACTIVE
                : event.getEligibilityMode();
    }
}
