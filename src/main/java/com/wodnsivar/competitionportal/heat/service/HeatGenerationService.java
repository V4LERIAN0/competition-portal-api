package com.wodnsivar.competitionportal.heat.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.common.exception.BadRequestException;
import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.enums.AthleteStatus;
import com.wodnsivar.competitionportal.enums.CheckInStatus;
import com.wodnsivar.competitionportal.enums.HeatStatus;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.heat.dto.GenerateRandomHeatsRequest;
import com.wodnsivar.competitionportal.heat.dto.HeatResponse;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeat;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeatAthlete;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatAthleteRepository;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class HeatGenerationService {
    private final HeatService heatService;
    private final CompetitionHeatRepository heatRepository;
    private final CompetitionHeatAthleteRepository assignmentRepository;
    private final CompetitionAthleteRepository athleteRepository;

    public List<HeatResponse> generateRandom(Long eventId, GenerateRandomHeatsRequest request) {
        CompetitionEvent event = heatService.findEvent(eventId);
        if (heatRepository.existsByEventId(eventId)) {
            throw new ConflictException("This event already has heats. Review or remove them before generation.");
        }
        List<CompetitionAthlete> athletes = request.categoryId() == null
                ? athleteRepository.findByCompetitionIdAndStatusNotInOrderByFullNameAsc(
                        event.getCompetition().getId(), List.of(AthleteStatus.WITHDRAWN, AthleteStatus.DISQUALIFIED))
                : athleteRepository.findByCompetitionIdAndCategoryIdOrderByFullNameAsc(
                        event.getCompetition().getId(), request.categoryId()).stream()
                        .filter(a -> a.getStatus() != AthleteStatus.WITHDRAWN && a.getStatus() != AthleteStatus.DISQUALIFIED)
                        .toList();
        if (athletes.isEmpty()) throw new BadRequestException("No eligible athletes were found for heat generation.");

        List<CompetitionAthlete> shuffled = new ArrayList<>(athletes);
        Collections.shuffle(shuffled, request.randomSeed() == null ? new Random() : new Random(request.randomSeed()));
        int startingNumber = request.startingHeatNumber() == null ? 1 : request.startingHeatNumber();
        int interval = request.minutesBetweenHeats() == null ? 10 : request.minutesBetweenHeats();
        List<CompetitionHeat> generated = new ArrayList<>();

        for (int offset = 0; offset < shuffled.size(); offset += request.capacity()) {
            int index = generated.size();
            int heatNumber = startingNumber + index;
            CompetitionHeat heat = CompetitionHeat.builder()
                    .competition(event.getCompetition()).event(event).name("Heat " + heatNumber)
                    .heatNumber(heatNumber)
                    .scheduledTime(request.firstHeatTime() == null ? null : request.firstHeatTime().plusMinutes((long) index * interval))
                    .status(HeatStatus.SCHEDULED).capacity(request.capacity()).displayOrder(heatNumber)
                    .publicVisible(Boolean.TRUE.equals(request.publicVisible())).build();
            heat = heatRepository.save(heat);
            List<CompetitionAthlete> group = shuffled.subList(offset, Math.min(offset + request.capacity(), shuffled.size()));
            for (int lane = 1; lane <= group.size(); lane++) {
                CompetitionHeatAthlete assignment = CompetitionHeatAthlete.builder()
                        .heat(heat).athlete(group.get(lane - 1)).laneNumber(lane).stationNumber(lane)
                        .checkInStatus(CheckInStatus.NOT_OPEN).build();
                heat.getAssignments().add(assignment);
                assignmentRepository.save(assignment);
            }
            generated.add(heat);
        }
        return heatRepository.findByEventIdOrderByDisplayOrderAscHeatNumberAsc(eventId)
                .stream().map(heatService::toResponse).toList();
    }
}
