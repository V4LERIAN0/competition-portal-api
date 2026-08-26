package com.wodnsivar.competitionportal.score.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.auth.security.UserPrincipal;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.*;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import com.wodnsivar.competitionportal.score.dto.ScoreEntryRequest;
import com.wodnsivar.competitionportal.score.dto.ScoreResponse;
import com.wodnsivar.competitionportal.score.repository.CompetitionScoreRepository;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import com.wodnsivar.competitionportal.user.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScoreMutationTimestampIntegrationTest {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private CompetitionScoreRepository scoreRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionCategoryRepository categoryRepository;

    @Autowired
    private CompetitionAthleteRepository athleteRepository;

    @Autowired
    private CompetitionEventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    private Long eventId;
    private Long athleteId;

    @BeforeEach
    void setUp() {
        UserAccount admin = userRepository.saveAndFlush(UserAccount.builder()
                .email("timestamp-test@sivarfest.fit")
                .passwordHash("{noop}test")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());

        Competition competition = competitionRepository.saveAndFlush(
                Competition.builder()
                        .name("Timestamp Test Competition")
                        .slug("timestamp-test")
                        .registrationStatus(RegistrationStatus.CLOSED)
                        .visibilityStatus(VisibilityStatus.PRIVATE)
                        .status(CompetitionStatus.DRAFT)
                        .checkInOpenMinutesBeforeHeat(30)
                        .build()
        );

        CompetitionCategory category = categoryRepository.saveAndFlush(
                CompetitionCategory.builder()
                        .competition(competition)
                        .name("Male Rx")
                        .genderClassification(GenderClassification.MALE)
                        .divisionLabel("Rx")
                        .displayOrder(1)
                        .active(true)
                        .build()
        );

        CompetitionAthlete athlete = athleteRepository.saveAndFlush(
                CompetitionAthlete.builder()
                        .competition(competition)
                        .category(category)
                        .fullName("Timestamp Athlete")
                        .bibNumber("T001")
                        .status(AthleteStatus.CONFIRMED)
                        .checkedIn(false)
                        .build()
        );

        CompetitionEvent event = eventRepository.saveAndFlush(
                CompetitionEvent.builder()
                        .competition(competition)
                        .eventCode("T1")
                        .name("Timestamp Max Lift")
                        .scoreType(ScoreType.MAX_WEIGHT)
                        .rankingDirection(RankingDirection.HIGHER_IS_BETTER)
                        .cappedScoringEnabled(false)
                        .tiebreakType(TiebreakType.NONE)
                        .tiebreakRequired(false)
                        .displayOrder(1)
                        .publicVisible(false)
                        .scoreVisible(false)
                        .status(EventStatus.DRAFT)
                        .build()
        );

        UserPrincipal principal = new UserPrincipal(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        eventId = event.getId();
        athleteId = athlete.getId();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminUpdateResponseContainsPersistedMutationTimestamp()
            throws InterruptedException {
        ScoreResponse created = createDraftScore();

        prepareForNextMutation();

        ScoreResponse updated = scoreService.adminUpsert(
                eventId,
                athleteId,
                maxWeightRequest("315")
        );

        assertCurrentMutationTimestamp(updated, created.updatedAt());
    }

    @Test
    void validateResponseContainsPersistedMutationTimestamp()
            throws InterruptedException {
        ScoreResponse created = createDraftScore();

        prepareForNextMutation();

        ScoreResponse validated = scoreService.validate(created.id());

        assertThat(validated.status()).isEqualTo(ScoreStatus.VALIDATED);
        assertThat(validated.validatedAt()).isNotNull();
        assertCurrentMutationTimestamp(validated, created.updatedAt());
    }

    @Test
    void publishResponseContainsPersistedMutationTimestamp()
            throws InterruptedException {
        ScoreResponse created = createDraftScore();

        prepareForNextMutation();

        ScoreResponse validated = scoreService.validate(created.id());
        assertCurrentMutationTimestamp(validated, created.updatedAt());

        prepareForNextMutation();

        ScoreResponse published = scoreService.publish(created.id());

        assertThat(published.status()).isEqualTo(ScoreStatus.PUBLISHED);
        assertThat(published.publishedAt()).isNotNull();
        assertCurrentMutationTimestamp(published, validated.updatedAt());
    }

    private ScoreEntryRequest maxWeightRequest(String weight) {
        return new ScoreEntryRequest(
                null,
                null,
                null,
                new BigDecimal(weight),
                null,
                null,
                null,
                null
        );
    }
    private ScoreResponse createDraftScore() {
        return scoreService.adminUpsert(
                eventId,
                athleteId,
                maxWeightRequest("300")
        );
    }

    private void prepareForNextMutation() throws InterruptedException {
        entityManager.flush();
        entityManager.clear();
        Thread.sleep(10);
    }

    private void assertCurrentMutationTimestamp(
            ScoreResponse response,
            Instant previousTimestamp
    ) {
        entityManager.flush();
        entityManager.clear();

        Instant persistedTimestamp = scoreRepository.findById(response.id())
                .orElseThrow()
                .getUpdatedAt();

        long timestampDifferenceNanos = Math.abs(
                Duration.between(
                        response.updatedAt(),
                        persistedTimestamp
                ).toNanos()
        );

        assertThat(timestampDifferenceNanos)
                .isLessThanOrEqualTo(1_000L);
        assertThat(response.updatedAt()).isAfter(previousTimestamp);
    }
}