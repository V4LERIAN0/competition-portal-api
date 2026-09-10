package com.wodnsivar.competitionportal.heat.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.enums.AthleteStatus;
import com.wodnsivar.competitionportal.enums.GenderClassification;
import com.wodnsivar.competitionportal.enums.HeatSeedingMode;
import com.wodnsivar.competitionportal.enums.HeatStatus;
import com.wodnsivar.competitionportal.enums.LeaderboardStatus;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.service.EventEligibilityService;
import com.wodnsivar.competitionportal.heat.dto.GenerateHeatsRequest;
import com.wodnsivar.competitionportal.heat.dto.HeatCategoryScheduleRequest;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeat;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatAthleteRepository;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatRepository;
import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardResponse;
import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardRow;
import com.wodnsivar.competitionportal.leaderboard.service.EventRankingService;
import com.wodnsivar.competitionportal.leaderboard.service.OverallLeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatGenerationServiceTest {

    @Mock private HeatService heatService;
    @Mock private CompetitionHeatRepository heatRepository;
    @Mock private CompetitionHeatAthleteRepository assignmentRepository;
    @Mock private CompetitionAthleteRepository athleteRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private EventEligibilityService eventEligibility;
    @Mock private EventRankingService eventRankingService;
    @Mock private OverallLeaderboardService overallLeaderboardService;

    private HeatGenerationService generationService;
    private Competition competition;
    private CompetitionEvent event;

    @BeforeEach
    void setUp() {
        generationService = new HeatGenerationService(
                heatService,
                heatRepository,
                assignmentRepository,
                athleteRepository,
                categoryRepository,
                eventEligibility,
                eventRankingService,
                overallLeaderboardService
        );
        competition = Competition.builder().id(1L).name("SIVARFEST").build();
        event = CompetitionEvent.builder().id(2L).competition(competition).eventCode("2").build();
    }

    @Test
    void balancesTheSivarfestCategorySizesWithoutExceedingTen() {
        assertThat(groupSizes(38)).containsExactly(9, 9, 10, 10);
        assertThat(groupSizes(40)).containsExactly(10, 10, 10, 10);
        assertThat(groupSizes(11)).containsExactly(5, 6);
        assertThat(groupSizes(32)).containsExactly(8, 8, 8, 8);
    }

    @Test
    void generatesSeparateCategoryBlocksWithIndependentStartTimes() {
        stubGenerationDefaults();
        CompetitionCategory femaleSc = category(1L, "Female SC", 1);
        CompetitionCategory femaleRx = category(2L, "Female RX", 3);
        List<CompetitionAthlete> athletes = new ArrayList<>();
        athletes.addAll(athletes(femaleSc, 38, 1));
        athletes.addAll(athletes(femaleRx, 11, 101));
        when(categoryRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(femaleRx, femaleSc));
        when(athleteRepository.findByCompetitionIdAndStatusNotInOrderByFullNameAsc(anyLong(), any()))
                .thenReturn(athletes);

        generationService.generate(2L, new GenerateHeatsRequest(
                HeatSeedingMode.RANDOM,
                List.of(
                        new HeatCategoryScheduleRequest(1L, LocalDateTime.of(2026, 9, 19, 8, 5)),
                        new HeatCategoryScheduleRequest(2L, LocalDateTime.of(2026, 9, 19, 10, 5))
                ),
                10,
                1,
                15,
                true,
                2026L,
                null
        ));

        ArgumentCaptor<CompetitionHeat> captor = ArgumentCaptor.forClass(CompetitionHeat.class);
        org.mockito.Mockito.verify(heatRepository, org.mockito.Mockito.times(6)).save(captor.capture());
        List<CompetitionHeat> heats = captor.getAllValues();
        assertThat(heats).extracting(heat -> heat.getAssignments().size())
                .containsExactly(9, 9, 10, 10, 5, 6);
        assertThat(heats).allSatisfy(heat -> assertThat(heat.getAssignments())
                .extracting(assignment -> assignment.getAthlete().getCategory().getId())
                .containsOnly(heat.getAssignments().getFirst().getAthlete().getCategory().getId()));
        assertThat(heats).extracting(CompetitionHeat::getScheduledTime)
                .containsExactly(
                        LocalDateTime.of(2026, 9, 19, 8, 5),
                        LocalDateTime.of(2026, 9, 19, 8, 20),
                        LocalDateTime.of(2026, 9, 19, 8, 35),
                        LocalDateTime.of(2026, 9, 19, 8, 50),
                        LocalDateTime.of(2026, 9, 19, 10, 5),
                        LocalDateTime.of(2026, 9, 19, 10, 20)
                );
    }

    @Test
    void eventStandingsPutTheHighestRankedAthletesInTheLastHeat() {
        stubGenerationDefaults();
        CompetitionCategory category = category(1L, "Female RX", 1);
        List<CompetitionAthlete> athletes = athletes(category, 11, 1);
        when(categoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(category));
        when(athleteRepository.findByCompetitionIdAndStatusNotInOrderByFullNameAsc(anyLong(), any()))
                .thenReturn(athletes);
        when(eventRankingService.getAdminPreview(1L, 1L)).thenReturn(eventLeaderboard(category, athletes));

        generationService.generate(2L, new GenerateHeatsRequest(
                HeatSeedingMode.EVENT_STANDINGS,
                List.of(new HeatCategoryScheduleRequest(1L, null)),
                10,
                1,
                13,
                true,
                null,
                1L
        ));

        ArgumentCaptor<CompetitionHeat> captor = ArgumentCaptor.forClass(CompetitionHeat.class);
        org.mockito.Mockito.verify(heatRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<CompetitionHeat> heats = captor.getAllValues();
        assertThat(heats.get(0).getAssignments())
                .extracting(assignment -> assignment.getAthlete().getId())
                .containsExactly(11L, 10L, 9L, 8L, 7L);
        assertThat(heats.get(1).getAssignments())
                .extracting(assignment -> assignment.getAthlete().getId())
                .containsExactly(6L, 5L, 4L, 3L, 2L, 1L);
    }

    private List<Integer> groupSizes(int athleteCount) {
        return HeatGenerationService.balancedGroups(
                        java.util.stream.IntStream.range(0, athleteCount).boxed().toList(),
                        10
                ).stream()
                .map(List::size)
                .toList();
    }

    private void stubGenerationDefaults() {
        when(heatService.findEvent(2L)).thenReturn(event);
        when(heatRepository.save(any(CompetitionHeat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventEligibility.explicitlyEligibleAthleteIds(event)).thenReturn(null);
    }

    private CompetitionCategory category(Long id, String name, int displayOrder) {
        return CompetitionCategory.builder()
                .id(id)
                .competition(competition)
                .name(name)
                .genderClassification(GenderClassification.FEMALE)
                .displayOrder(displayOrder)
                .active(true)
                .build();
    }

    private List<CompetitionAthlete> athletes(CompetitionCategory category, int count, int firstId) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> CompetitionAthlete.builder()
                        .id((long) firstId + index)
                        .competition(competition)
                        .category(category)
                        .fullName("Athlete " + (firstId + index))
                        .status(AthleteStatus.REGISTERED)
                        .build())
                .toList();
    }

    private EventLeaderboardResponse eventLeaderboard(
            CompetitionCategory category,
            List<CompetitionAthlete> athletes
    ) {
        List<EventLeaderboardRow> rows = athletes.stream()
                .map(athlete -> leaderboardRow(athlete, Math.toIntExact(athlete.getId())))
                .toList();
        return new EventLeaderboardResponse(
                1L,
                "SIVARFEST",
                "sivarfest-2026",
                1L,
                "1",
                "WOD 1",
                ScoreType.FOR_TIME,
                RankingDirection.LOWER_IS_BETTER,
                null,
                LeaderboardStatus.PUBLISHED,
                null,
                List.of(new EventLeaderboardResponse.CategoryLeaderboard(
                        category.getId(),
                        category.getName(),
                        category.getGenderClassification(),
                        category.getDivisionLabel(),
                        true,
                        rows
                ))
        );
    }

    private EventLeaderboardRow leaderboardRow(CompetitionAthlete athlete, int rank) {
        return new EventLeaderboardRow(
                rank,
                rank,
                false,
                1L,
                athlete.getId(),
                athlete.getFullName(),
                null,
                null,
                null,
                athlete.getCategory().getId(),
                athlete.getCategory().getName(),
                athlete.getId(),
                null,
                ScoreType.FOR_TIME,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
