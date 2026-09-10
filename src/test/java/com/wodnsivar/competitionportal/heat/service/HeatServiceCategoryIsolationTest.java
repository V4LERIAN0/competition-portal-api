package com.wodnsivar.competitionportal.heat.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.common.exception.BadRequestException;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import com.wodnsivar.competitionportal.event.service.EventEligibilityService;
import com.wodnsivar.competitionportal.heat.dto.HeatAssignmentRequest;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeat;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeatAthlete;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatAthleteRepository;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeatServiceCategoryIsolationTest {

    @Test
    void rejectsAddingAnAthleteFromAnotherCategory() {
        CompetitionHeatRepository heatRepository = mock(CompetitionHeatRepository.class);
        CompetitionHeatAthleteRepository assignmentRepository = mock(CompetitionHeatAthleteRepository.class);
        CompetitionEventRepository eventRepository = mock(CompetitionEventRepository.class);
        CompetitionAthleteRepository athleteRepository = mock(CompetitionAthleteRepository.class);
        CompetitionRepository competitionRepository = mock(CompetitionRepository.class);
        EventEligibilityService eventEligibility = mock(EventEligibilityService.class);
        HeatService service = new HeatService(
                heatRepository,
                assignmentRepository,
                eventRepository,
                athleteRepository,
                competitionRepository,
                eventEligibility
        );

        Competition competition = Competition.builder().id(1L).build();
        CompetitionCategory femaleSc = CompetitionCategory.builder()
                .id(1L).competition(competition).name("Female SC").build();
        CompetitionCategory maleSc = CompetitionCategory.builder()
                .id(2L).competition(competition).name("Male SC").build();
        CompetitionAthlete assignedAthlete = CompetitionAthlete.builder()
                .id(1L).competition(competition).category(femaleSc).fullName("Assigned").build();
        CompetitionAthlete otherCategoryAthlete = CompetitionAthlete.builder()
                .id(2L).competition(competition).category(maleSc).fullName("Other category").build();
        CompetitionEvent event = CompetitionEvent.builder().id(1L).competition(competition).build();
        CompetitionHeat heat = CompetitionHeat.builder()
                .id(1L)
                .competition(competition)
                .event(event)
                .capacity(10)
                .assignments(new ArrayList<>())
                .build();
        heat.getAssignments().add(CompetitionHeatAthlete.builder()
                .id(1L)
                .heat(heat)
                .athlete(assignedAthlete)
                .positionNumber(1)
                .build());

        when(heatRepository.findById(1L)).thenReturn(Optional.of(heat));
        when(athleteRepository.findById(2L)).thenReturn(Optional.of(otherCategoryAthlete));

        assertThatThrownBy(() -> service.assignAthlete(
                1L,
                new HeatAssignmentRequest(2L, 2, false)
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("different categories");
    }
}
