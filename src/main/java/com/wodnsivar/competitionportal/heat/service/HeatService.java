package com.wodnsivar.competitionportal.heat.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.common.exception.BadRequestException;
import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.common.exception.ResourceNotFoundException;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.CheckInStatus;
import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.HeatStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import com.wodnsivar.competitionportal.heat.dto.*;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeat;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeatAthlete;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatAthleteRepository;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HeatService {
    private final CompetitionHeatRepository heatRepository;
    private final CompetitionHeatAthleteRepository assignmentRepository;
    private final CompetitionEventRepository eventRepository;
    private final CompetitionAthleteRepository athleteRepository;
    private final CompetitionRepository competitionRepository;

    public HeatResponse createHeat(Long eventId, HeatCreateRequest request) {
        CompetitionEvent event = findEvent(eventId);
        deleteCancelledHeatUsingNumber(eventId, request.heatNumber());
        validateUniqueHeatNumber(eventId, request.heatNumber(), null);
        HeatStatus status = defaultIfNull(request.status(), HeatStatus.SCHEDULED);
        validatePersistedStatus(status);
        CompetitionHeat heat = CompetitionHeat.builder()
                .competition(event.getCompetition()).event(event)
                .name(request.name().trim()).heatNumber(request.heatNumber())
                .scheduledTime(request.scheduledTime())
                .status(status)
                .capacity(request.capacity()).notes(normalizeNullable(request.notes()))
                .displayOrder(defaultIfNull(request.displayOrder(), request.heatNumber()))
                .publicVisible(status == HeatStatus.CANCELLED
                        ? false
                        : defaultIfNull(request.publicVisible(), false)).build();
        return toResponse(heatRepository.save(heat));
    }

    @Transactional(readOnly = true)
    public List<HeatResponse> getEventHeats(Long eventId) {
        findEvent(eventId);
        return heatRepository.findByEventIdOrderByDisplayOrderAscHeatNumberAsc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public HeatResponse getHeat(Long heatId) { return toResponse(findHeat(heatId)); }

    public HeatResponse updateHeat(Long heatId, HeatUpdateRequest request) {
        CompetitionHeat heat = findHeat(heatId);
        validatePersistedStatus(request.status());
        validateUniqueHeatNumber(heat.getEvent().getId(), request.heatNumber(), heatId);
        validateActualTimes(request.actualStartTime(), request.actualEndTime());
        int assigned = heat.getAssignments().size();
        if (request.capacity() < assigned && !Boolean.TRUE.equals(request.allowCapacityOverride())) {
            throw new ConflictException("Capacity cannot be lower than the " + assigned +
                    " assigned athletes without confirming the override.");
        }
        heat.setName(request.name().trim());
        heat.setHeatNumber(request.heatNumber());
        heat.setScheduledTime(request.scheduledTime());
        heat.setActualStartTime(request.actualStartTime());
        heat.setActualEndTime(request.actualEndTime());
        heat.setStatus(request.status());
        heat.setCapacity(request.capacity());
        heat.setNotes(normalizeNullable(request.notes()));
        heat.setDisplayOrder(request.displayOrder());
        heat.setPublicVisible(request.status() == HeatStatus.CANCELLED ? false : request.publicVisible());
        return toResponse(heatRepository.save(heat));
    }

    public void cancelHeat(Long heatId) {
        heatRepository.delete(findHeat(heatId));
    }

    public HeatResponse assignAthlete(Long heatId, HeatAssignmentRequest request) {
        CompetitionHeat heat = findHeat(heatId);
        CompetitionAthlete athlete = athleteRepository.findById(request.athleteId())
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + request.athleteId()));
        validateAthleteForHeat(heat, athlete, null);
        validatePosition(heatId, request.positionNumber(), null);
        if (heat.getAssignments().size() >= heat.getCapacity()
                && !Boolean.TRUE.equals(request.allowCapacityOverride())) {
            throw new ConflictException("Heat is at capacity. Confirm the override to add another athlete.");
        }
        CompetitionHeatAthlete assignment = CompetitionHeatAthlete.builder()
                .heat(heat).athlete(athlete).positionNumber(request.positionNumber())
                .checkInStatus(CheckInStatus.NOT_OPEN).build();
        heat.getAssignments().add(assignment);
        assignmentRepository.save(assignment);
        return toResponse(heat);
    }

    public HeatResponse updateAssignment(Long assignmentId, HeatAssignmentRequest request) {
        CompetitionHeatAthlete assignment = findAssignment(assignmentId);
        CompetitionHeat heat = assignment.getHeat();
        CompetitionAthlete athlete = athleteRepository.findById(request.athleteId())
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + request.athleteId()));
        validateAthleteForHeat(heat, athlete, assignmentId);
        validatePosition(heat.getId(), request.positionNumber(), assignmentId);
        assignment.setAthlete(athlete);
        assignment.setPositionNumber(request.positionNumber());
        assignmentRepository.save(assignment);
        return toResponse(findHeat(heat.getId()));
    }

    public void removeAssignment(Long assignmentId) {
        assignmentRepository.delete(findAssignment(assignmentId));
    }

    @Transactional(readOnly = true)
    public List<HeatResponse> getPublicSchedule(String competitionSlug) {
        Competition competition = competitionRepository.findBySlug(competitionSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with slug: " + competitionSlug));
        if (competition.getVisibilityStatus() != VisibilityStatus.PUBLIC
                || competition.getStatus() == CompetitionStatus.DRAFT
                || competition.getStatus() == CompetitionStatus.ARCHIVED) {
            throw new ResourceNotFoundException("Competition not publicly available with slug: " + competitionSlug);
        }
        return heatRepository
                .findByCompetitionIdAndPublicVisibleTrueAndStatusNotOrderByScheduledTimeAscHeatNumberAsc(
                        competition.getId(), HeatStatus.CANCELLED)
                .stream().map(this::toResponse).toList();
    }

    CompetitionEvent findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    CompetitionHeat findHeat(Long heatId) {
        return heatRepository.findById(heatId)
                .orElseThrow(() -> new ResourceNotFoundException("Heat not found with id: " + heatId));
    }

    private CompetitionHeatAthlete findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Heat assignment not found with id: " + assignmentId));
    }

    private void validateUniqueHeatNumber(Long eventId, Integer heatNumber, Long heatId) {
        boolean exists = heatId == null
                ? heatRepository.existsByEventIdAndHeatNumber(eventId, heatNumber)
                : heatRepository.existsByEventIdAndHeatNumberAndIdNot(eventId, heatNumber, heatId);
        if (exists) throw new ConflictException("Heat number " + heatNumber + " already exists for this event.");
    }

    private void deleteCancelledHeatUsingNumber(Long eventId, Integer heatNumber) {
        heatRepository.findByEventIdAndHeatNumber(eventId, heatNumber)
                .filter(heat -> heat.getStatus() == HeatStatus.CANCELLED)
                .ifPresent(heatRepository::delete);
    }

    private void validateAthleteForHeat(CompetitionHeat heat, CompetitionAthlete athlete, Long assignmentId) {
        if (!athlete.getCompetition().getId().equals(heat.getCompetition().getId())) {
            throw new BadRequestException("The athlete and heat must belong to the same competition.");
        }
        boolean alreadyAssigned = assignmentRepository.existsForEventAndAthleteExcludingHeatStatus(
                heat.getEvent().getId(), athlete.getId(), HeatStatus.CANCELLED);
        if (alreadyAssigned) {
            CompetitionHeatAthlete current = assignmentId == null ? null : assignmentRepository.findById(assignmentId).orElse(null);
            if (current == null || !current.getAthlete().getId().equals(athlete.getId())) {
                throw new ConflictException("Athlete is already assigned to a heat for this event.");
            }
        }
    }

    private void validatePosition(Long heatId, Integer position, Long assignmentId) {
        boolean exists = assignmentId == null
                ? assignmentRepository.existsByHeatIdAndPositionNumber(heatId, position)
                : assignmentRepository.existsByHeatIdAndPositionNumberAndIdNot(heatId, position, assignmentId);
        if (exists) throw new ConflictException(
                "Position " + position + " is already assigned in this heat.");
    }

    private void validateActualTimes(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("Actual end time cannot be before actual start time.");
        }
    }

    private void validatePersistedStatus(HeatStatus status) {
        if (status == HeatStatus.CANCELLED) {
            throw new BadRequestException(
                    "Cancelled heats are deleted. Use the heat DELETE endpoint instead.");
        }
    }

    HeatResponse toResponse(CompetitionHeat heat) {
        List<HeatAssignmentResponse> assignments = heat.getAssignments().stream().map(a ->
                new HeatAssignmentResponse(a.getId(), a.getAthlete().getId(), a.getAthlete().getFullName(),
                        a.getAthlete().getBibNumber(), a.getAthlete().getCategory().getId(),
                        a.getAthlete().getCategory().getName(), a.getPositionNumber(),
                        a.getCheckInStatus(), a.getCheckInTime())).toList();
        return new HeatResponse(heat.getId(), heat.getCompetition().getId(), heat.getEvent().getId(),
                heat.getEvent().getEventCode(), heat.getEvent().getName(), heat.getName(), heat.getHeatNumber(),
                heat.getScheduledTime(), heat.getActualStartTime(), heat.getActualEndTime(), heat.getStatus(),
                heat.getCapacity(), assignments.size(), heat.getNotes(), heat.getDisplayOrder(),
                heat.getPublicVisible(), assignments);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private <T> T defaultIfNull(T value, T fallback) { return value == null ? fallback : value; }
}
