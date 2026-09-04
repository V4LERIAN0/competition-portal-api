package com.wodnsivar.competitionportal.leaderboard.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.common.exception.ResourceNotFoundException;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.AthleteStatus;
import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.EventStatus;
import com.wodnsivar.competitionportal.enums.LeaderboardStatus;
import com.wodnsivar.competitionportal.enums.ScoreStatus;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.enums.TiebreakType;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import com.wodnsivar.competitionportal.enums.WeightUnit;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import com.wodnsivar.competitionportal.event.service.EffectiveEventConfiguration;
import com.wodnsivar.competitionportal.event.service.EventConfigurationResolver;
import com.wodnsivar.competitionportal.event.service.EventEligibilityService;
import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardResponse;
import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardRow;
import com.wodnsivar.competitionportal.score.entity.CompetitionScore;
import com.wodnsivar.competitionportal.score.repository.CompetitionScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventRankingService {

    static final Set<ScoreStatus> ADMIN_VISIBLE_SCORE_STATUSES = EnumSet.of(
            ScoreStatus.DRAFT,
            ScoreStatus.SUBMITTED,
            ScoreStatus.VALIDATED,
            ScoreStatus.PUBLISHED,
            ScoreStatus.LOCKED
    );

    static final Set<ScoreStatus> PUBLIC_SCORE_STATUSES = EnumSet.of(
            ScoreStatus.PUBLISHED,
            ScoreStatus.LOCKED
    );

    private final CompetitionEventRepository eventRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final CompetitionAthleteRepository athleteRepository;
    private final CompetitionScoreRepository scoreRepository;
    private final TieBreakService tieBreakService;
    private final EventConfigurationResolver eventConfigurations;
    private final EventEligibilityService eventEligibility;

    public EventLeaderboardResponse getAdminPreview(Long competitionId, Long eventId) {
        Competition competition = findCompetition(competitionId);
        CompetitionEvent event = findEventInCompetition(eventId, competitionId);
        List<CompetitionCategory> categories = categoryRepository
                .findByCompetitionIdOrderByDisplayOrderAscNameAsc(competitionId);
        List<CompetitionAthlete> athletes = eligibleAthletes(competitionId);
        List<CompetitionScore> scores = scoreRepository
                .findByEventIdAndStatusIn(eventId, ADMIN_VISIBLE_SCORE_STATUSES);

        return buildResponse(competition, event, categories, athletes, scores);
    }

    public EventLeaderboardResponse getPublicLeaderboard(String competitionSlug, Long eventId) {
        Competition competition = findPublicCompetition(competitionSlug);
        CompetitionEvent event = findEventInCompetition(eventId, competition.getId());
        if (!Boolean.TRUE.equals(event.getPublicVisible())
                || !Boolean.TRUE.equals(event.getScoreVisible())
                || event.getStatus() == EventStatus.DRAFT) {
            throw new ResourceNotFoundException("Event leaderboard is not publicly available.");
        }

        List<CompetitionCategory> categories = categoryRepository
                .findByCompetitionIdAndActiveTrueOrderByDisplayOrderAscNameAsc(competition.getId());
        List<CompetitionAthlete> athletes = eligibleAthletes(competition.getId());
        List<CompetitionScore> scores = scoreRepository
                .findByEventIdAndStatusIn(eventId, PUBLIC_SCORE_STATUSES);

        return buildResponse(competition, event, categories, athletes, scores);
    }

    List<EventLeaderboardRow> rankForCategory(
            CompetitionEvent event,
            CompetitionCategory category,
            List<CompetitionAthlete> competitionAthletes,
            List<CompetitionScore> availableScores
    ) {
        Set<Long> explicitlyEligibleAthleteIds = eventEligibility.explicitlyEligibleAthleteIds(event);
        List<CompetitionAthlete> categoryAthletes = competitionAthletes.stream()
                .filter(this::isEligible)
                .filter(athlete -> explicitlyEligibleAthleteIds == null
                        || explicitlyEligibleAthleteIds.contains(athlete.getId()))
                .filter(athlete -> category.getId().equals(athlete.getCategory().getId()))
                .sorted(athleteNameOrder())
                .toList();

        Map<Long, CompetitionScore> scoreByAthleteId = availableScores.stream()
                .filter(score -> event.getId().equals(score.getEvent().getId()))
                .filter(score -> category.getId().equals(score.getAthlete().getCategory().getId()))
                .collect(Collectors.toMap(
                        score -> score.getAthlete().getId(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        List<CompetitionScore> rankedScores = categoryAthletes.stream()
                .map(athlete -> scoreByAthleteId.get(athlete.getId()))
                .filter(score -> score != null)
                .sorted(scoreOrder(event))
                .toList();

        Map<Long, Integer> rankByScoreId = new HashMap<>();
        Map<Integer, Integer> rankFrequency = new HashMap<>();
        CompetitionScore previous = null;
        int currentRank = 0;

        for (int index = 0; index < rankedScores.size(); index++) {
            CompetitionScore score = rankedScores.get(index);
            if (previous == null || tieBreakService.compare(event, previous, score) != 0) {
                currentRank = index + 1;
            }
            rankByScoreId.put(score.getId(), currentRank);
            rankFrequency.merge(currentRank, 1, Integer::sum);
            previous = score;
        }

        List<EventLeaderboardRow> rows = new ArrayList<>();
        for (CompetitionScore score : rankedScores) {
            int rank = rankByScoreId.get(score.getId());
            rows.add(toRow(event, score.getAthlete(), score, rank, rank,
                    rankFrequency.getOrDefault(rank, 0) > 1));
        }

        categoryAthletes.stream()
                .filter(athlete -> !scoreByAthleteId.containsKey(athlete.getId()))
                .map(athlete -> toRow(event, athlete, null, null, null, false))
                .forEach(rows::add);

        return List.copyOf(rows);
    }

    LeaderboardStatus leaderboardStatus(Collection<CompetitionScore> scores) {
        if (scores.isEmpty()) {
            return LeaderboardStatus.UNOFFICIAL;
        }
        if (scores.stream().allMatch(score -> score.getStatus() == ScoreStatus.LOCKED)) {
            return LeaderboardStatus.FINAL;
        }
        if (scores.stream().allMatch(score -> score.getStatus() == ScoreStatus.PUBLISHED
                || score.getStatus() == ScoreStatus.LOCKED)) {
            return LeaderboardStatus.PUBLISHED;
        }
        if (scores.stream().anyMatch(score -> score.getStatus() == ScoreStatus.DRAFT)) {
            return LeaderboardStatus.UNOFFICIAL;
        }
        return LeaderboardStatus.UNDER_REVIEW;
    }

    private EventLeaderboardResponse buildResponse(
            Competition competition,
            CompetitionEvent event,
            List<CompetitionCategory> categories,
            List<CompetitionAthlete> athletes,
            List<CompetitionScore> scores
    ) {
        List<EventLeaderboardResponse.CategoryLeaderboard> categoryBoards = categories.stream()
                .map(category -> new EventLeaderboardResponse.CategoryLeaderboard(
                        category.getId(),
                        category.getName(),
                        category.getGenderClassification(),
                        category.getDivisionLabel(),
                        category.getActive(),
                        rankForCategory(event, category, athletes, scores)
                ))
                .toList();

        return new EventLeaderboardResponse(
                competition.getId(),
                competition.getName(),
                competition.getSlug(),
                event.getId(),
                event.getEventCode(),
                event.getName(),
                event.getScoreType(),
                event.getRankingDirection(),
                event.getWeightUnit(),
                leaderboardStatus(scores),
                latestUpdate(event, scores),
                categoryBoards
        );
    }

    private EventLeaderboardRow toRow(
            CompetitionEvent event,
            CompetitionAthlete athlete,
            CompetitionScore score,
            Integer rank,
            Integer placementPoints,
            boolean tied
    ) {
        return new EventLeaderboardRow(
                rank,
                placementPoints,
                tied,
                event.getId(),
                athlete.getId(),
                athlete.getFullName(),
                athlete.getBibNumber(),
                athlete.getCountry(),
                athlete.getGymName(),
                athlete.getCategory().getId(),
                athlete.getCategory().getName(),
                score == null ? null : score.getId(),
                score == null ? null : score.getStatus(),
                event.getScoreType(),
                displayScore(event, athlete, score),
                score == null ? null : score.getCompleted(),
                score == null ? null : score.getScoreSeconds(),
                score == null ? null : score.getReps(),
                score == null ? null : score.getWeightValue(),
                event.getWeightUnit(),
                score == null ? null : score.getPointsValue(),
                score == null ? null : score.getCustomValue(),
                displayTiebreak(event, score),
                score == null ? null : score.getTiebreakValue(),
                event.getTiebreakType() == null ? TiebreakType.NONE : event.getTiebreakType()
        );
    }

    private Comparator<CompetitionScore> scoreOrder(CompetitionEvent event) {
        return (left, right) -> {
            int competitiveComparison = tieBreakService.compare(event, left, right);
            if (competitiveComparison != 0) {
                return competitiveComparison;
            }
            int nameComparison = compareNames(left.getAthlete().getFullName(), right.getAthlete().getFullName());
            if (nameComparison != 0) {
                return nameComparison;
            }
            return Comparator.nullsLast(Long::compareTo)
                    .compare(left.getAthlete().getId(), right.getAthlete().getId());
        };
    }

    private Comparator<CompetitionAthlete> athleteNameOrder() {
        return (left, right) -> {
            int nameComparison = compareNames(left.getFullName(), right.getFullName());
            if (nameComparison != 0) {
                return nameComparison;
            }
            return Comparator.nullsLast(Long::compareTo).compare(left.getId(), right.getId());
        };
    }

    private String displayScore(
            CompetitionEvent event,
            CompetitionAthlete athlete,
            CompetitionScore score
    ) {
        if (score == null) {
            return "No score";
        }

        EffectiveEventConfiguration configuration = eventConfigurations.resolve(
                event, athlete.getCategory());

        return switch (event.getScoreType()) {
            case FOR_TIME -> Boolean.TRUE.equals(score.getCompleted())
                    ? formatSeconds(score.getScoreSeconds())
                    : "CAP + " + score.getReps() + " reps";
            case AMRAP_REPS -> formatAmrap(score.getReps(), configuration.repsPerRound());
            case MAX_WEIGHT -> decimalText(score.getWeightValue()) + " " + weightLabel(event.getWeightUnit());
            case EMOM_REPS -> score.getReps() + " reps";
            case ROUNDS_COMPLETED -> score.getReps() + " rounds";
            case POINTS -> decimalText(score.getPointsValue()) + " pts";
            case CUSTOM -> decimalText(score.getCustomValue());
        };
    }

    private String displayTiebreak(CompetitionEvent event, CompetitionScore score) {
        if (score == null || score.getTiebreakValue() == null) {
            return null;
        }

        TiebreakType type = event.getTiebreakType() == null
                ? TiebreakType.NONE
                : event.getTiebreakType();
        return switch (type) {
            case NONE -> null;
            case TIME -> formatSeconds(score.getTiebreakValue().intValue());
            case REPS -> decimalText(score.getTiebreakValue()) + " reps";
            case WEIGHT -> decimalText(score.getTiebreakValue()) + " "
                    + weightLabel(event.getTiebreakWeightUnit());
            case POINTS -> decimalText(score.getTiebreakValue()) + " pts";
            case CUSTOM_NUMERIC -> decimalText(score.getTiebreakValue());
        };
    }

    private String formatAmrap(Integer reps, Integer repsPerRound) {
        if (reps == null) {
            return "No score";
        }
        if (repsPerRound == null || repsPerRound <= 0) {
            return reps + " reps";
        }
        int rounds = reps / repsPerRound;
        int remainingReps = reps % repsPerRound;
        return rounds + " rounds + " + remainingReps + " reps";
    }

    private String formatSeconds(Integer totalSeconds) {
        if (totalSeconds == null) {
            return "No score";
        }
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%d:%02d", minutes, seconds);
    }

    private String decimalText(BigDecimal value) {
        if (value == null) {
            return "No score";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String weightLabel(WeightUnit unit) {
        if (unit == WeightUnit.KILOGRAMS) {
            return "kg";
        }
        if (unit == WeightUnit.POUNDS) {
            return "lb";
        }
        return "";
    }

    private Instant latestUpdate(CompetitionEvent event, List<CompetitionScore> scores) {
        Instant latest = event.getUpdatedAt();
        for (CompetitionScore score : scores) {
            if (score.getUpdatedAt() != null && (latest == null || score.getUpdatedAt().isAfter(latest))) {
                latest = score.getUpdatedAt();
            }
        }
        return latest;
    }

    private List<CompetitionAthlete> eligibleAthletes(Long competitionId) {
        return athleteRepository.findByCompetitionIdAndStatusNotInOrderByFullNameAsc(
                competitionId,
                List.of(AthleteStatus.WITHDRAWN, AthleteStatus.DISQUALIFIED)
        );
    }

    private boolean isEligible(CompetitionAthlete athlete) {
        return athlete.getStatus() != AthleteStatus.WITHDRAWN
                && athlete.getStatus() != AthleteStatus.DISQUALIFIED;
    }

    private Competition findCompetition(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Competition not found with id: " + competitionId));
    }

    private Competition findPublicCompetition(String slug) {
        Competition competition = competitionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Competition not found with slug: " + slug));
        if (competition.getVisibilityStatus() != VisibilityStatus.PUBLIC
                || competition.getStatus() == CompetitionStatus.DRAFT
                || competition.getStatus() == CompetitionStatus.ARCHIVED) {
            throw new ResourceNotFoundException(
                    "Competition not publicly available with slug: " + slug);
        }
        return competition;
    }

    private CompetitionEvent findEventInCompetition(Long eventId, Long competitionId) {
        CompetitionEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (!competitionId.equals(event.getCompetition().getId())) {
            throw new ResourceNotFoundException("Event not found in competition: " + competitionId);
        }
        return event;
    }

    private int compareNames(String left, String right) {
        return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER).compare(left, right);
    }
}
