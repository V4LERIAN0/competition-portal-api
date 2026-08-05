package com.wodnsivar.competitionportal.leaderboard.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.AthleteStatus;
import com.wodnsivar.competitionportal.enums.GenderClassification;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreStatus;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.enums.WeightUnit;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardRow;
import com.wodnsivar.competitionportal.leaderboard.dto.OverallLeaderboardResponse;
import com.wodnsivar.competitionportal.score.entity.CompetitionScore;
import com.wodnsivar.competitionportal.score.repository.CompetitionScoreRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaderboardRankingTest {

    private final TieBreakService tieBreakService = new TieBreakService();

    @Test
    void maxWeightRanksPositiveLiftAboveZeroLift() {
        TestData data = maxWeightData(new BigDecimal("317.5"), BigDecimal.ZERO);
        EventRankingService service = eventRankingService();

        List<EventLeaderboardRow> rows = service.rankForCategory(
                data.event(),
                data.category(),
                List.of(data.carlos(), data.diego()),
                List.of(data.carlosScore(), data.diegoScore())
        );

        assertThat(rows).extracting(EventLeaderboardRow::athleteName)
                .containsExactly("Carlos Mendoza", "Diego Hernández");
        assertThat(rows).extracting(EventLeaderboardRow::rank)
                .containsExactly(1, 2);
        assertThat(rows).extracting(EventLeaderboardRow::placementPoints)
                .containsExactly(1, 2);
        assertThat(rows.get(0).scoreDisplay()).isEqualTo("317.5 lb");
        assertThat(rows.get(1).scoreDisplay()).isEqualTo("0 lb");
    }

    @Test
    void equalScoresShareRankAndSkipTheFollowingRank() {
        TestData data = maxWeightData(new BigDecimal("300.000"), new BigDecimal("300"));
        CompetitionAthlete thirdAthlete = athlete(8L, "Mario López", data.competition(), data.category());
        CompetitionScore thirdScore = score(
                8L,
                data.event(),
                thirdAthlete,
                new BigDecimal("250")
        );

        List<EventLeaderboardRow> rows = eventRankingService().rankForCategory(
                data.event(),
                data.category(),
                List.of(data.carlos(), data.diego(), thirdAthlete),
                List.of(data.carlosScore(), data.diegoScore(), thirdScore)
        );

        assertThat(rows).extracting(EventLeaderboardRow::rank)
                .containsExactly(1, 1, 3);
        assertThat(rows).extracting(EventLeaderboardRow::placementPoints)
                .containsExactly(1, 1, 3);
        assertThat(rows).extracting(EventLeaderboardRow::tied)
                .containsExactly(true, true, false);
    }

    @Test
    void adminOverallPreviewProducesTheExpectedCarlosAndDiegoRanking() {
        TestData data = maxWeightData(new BigDecimal("317.5"), BigDecimal.ZERO);
        CompetitionRepository competitionRepository = mock(CompetitionRepository.class);
        CompetitionEventRepository eventRepository = mock(CompetitionEventRepository.class);
        CompetitionCategoryRepository categoryRepository = mock(CompetitionCategoryRepository.class);
        CompetitionAthleteRepository athleteRepository = mock(CompetitionAthleteRepository.class);
        CompetitionScoreRepository scoreRepository = mock(CompetitionScoreRepository.class);

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(data.competition()));
        when(eventRepository.findByCompetitionIdOrderByDisplayOrderAscEventCodeAsc(1L))
                .thenReturn(List.of(data.event()));
        when(categoryRepository.findByCompetitionIdOrderByDisplayOrderAscNameAsc(1L))
                .thenReturn(List.of(data.category()));
        when(athleteRepository.findByCompetitionIdAndStatusNotInOrderByFullNameAsc(
                org.mockito.ArgumentMatchers.eq(1L), anyCollection()))
                .thenReturn(List.of(data.carlos(), data.diego()));
        when(scoreRepository.findByEventCompetitionIdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(1L), anyCollection()))
                .thenReturn(List.of(data.carlosScore(), data.diegoScore()));

        OverallLeaderboardService service = new OverallLeaderboardService(
                competitionRepository,
                eventRepository,
                categoryRepository,
                athleteRepository,
                scoreRepository,
                eventRankingService()
        );

        OverallLeaderboardResponse response = service.getAdminPreview(1L);

        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).rows())
                .extracting(row -> row.athleteName() + ":" + row.rank() + ":" + row.totalPoints())
                .containsExactly("Carlos Mendoza:1:1", "Diego Hernández:2:2");
    }

    private EventRankingService eventRankingService() {
        return new EventRankingService(null, null, null, null, null, tieBreakService);
    }

    private TestData maxWeightData(BigDecimal carlosWeight, BigDecimal diegoWeight) {
        Competition competition = Competition.builder()
                .id(1L)
                .name("SIVARFEST 2026")
                .slug("sivarfest-2026")
                .build();
        CompetitionCategory category = CompetitionCategory.builder()
                .id(2L)
                .competition(competition)
                .name("Male Rx")
                .genderClassification(GenderClassification.MALE)
                .divisionLabel("Rx")
                .displayOrder(1)
                .active(true)
                .build();
        CompetitionEvent event = CompetitionEvent.builder()
                .id(3L)
                .competition(competition)
                .eventCode("2B")
                .name("Max Clean")
                .scoreType(ScoreType.MAX_WEIGHT)
                .rankingDirection(RankingDirection.HIGHER_IS_BETTER)
                .weightUnit(WeightUnit.POUNDS)
                .tiebreakType(TiebreakType.NONE)
                .displayOrder(3)
                .build();
        CompetitionAthlete carlos = athlete(2L, "Carlos Mendoza", competition, category);
        CompetitionAthlete diego = athlete(7L, "Diego Hernández", competition, category);
        CompetitionScore carlosScore = score(6L, event, carlos, carlosWeight);
        CompetitionScore diegoScore = score(7L, event, diego, diegoWeight);
        return new TestData(
                competition,
                category,
                event,
                carlos,
                diego,
                carlosScore,
                diegoScore
        );
    }

    private CompetitionAthlete athlete(
            Long id,
            String name,
            Competition competition,
            CompetitionCategory category
    ) {
        return CompetitionAthlete.builder()
                .id(id)
                .competition(competition)
                .category(category)
                .fullName(name)
                .status(AthleteStatus.CONFIRMED)
                .build();
    }

    private CompetitionScore score(
            Long id,
            CompetitionEvent event,
            CompetitionAthlete athlete,
            BigDecimal weight
    ) {
        return CompetitionScore.builder()
                .id(id)
                .event(event)
                .athlete(athlete)
                .status(ScoreStatus.PUBLISHED)
                .weightValue(weight)
                .build();
    }

    private record TestData(
            Competition competition,
            CompetitionCategory category,
            CompetitionEvent event,
            CompetitionAthlete carlos,
            CompetitionAthlete diego,
            CompetitionScore carlosScore,
            CompetitionScore diegoScore
    ) {
    }
}
