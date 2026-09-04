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
import com.wodnsivar.competitionportal.enums.ScoreStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import com.wodnsivar.competitionportal.leaderboard.dto.AthleteLeaderboardPositionResponse;
import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardRow;
import com.wodnsivar.competitionportal.leaderboard.dto.OverallLeaderboardResponse;
import com.wodnsivar.competitionportal.leaderboard.dto.OverallLeaderboardRow;
import com.wodnsivar.competitionportal.score.entity.CompetitionScore;
import com.wodnsivar.competitionportal.score.repository.CompetitionScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
public class OverallLeaderboardService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionEventRepository eventRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final CompetitionAthleteRepository athleteRepository;
    private final CompetitionScoreRepository scoreRepository;
    private final EventRankingService eventRankingService;

    public OverallLeaderboardResponse getAdminPreview(Long competitionId) {
        Competition competition = findCompetition(competitionId);
        List<CompetitionEvent> events = eventRepository
                .findByCompetitionIdOrderByDisplayOrderAscEventCodeAsc(competitionId);
        List<CompetitionCategory> categories = categoryRepository
                .findByCompetitionIdOrderByDisplayOrderAscNameAsc(competitionId);
        return buildLeaderboard(
                competition,
                events,
                categories,
                EventRankingService.ADMIN_VISIBLE_SCORE_STATUSES
        );
    }

    public OverallLeaderboardResponse getPublicLeaderboard(String competitionSlug) {
        Competition competition = findPublicCompetition(competitionSlug);
        List<CompetitionEvent> events = eventRepository
                .findByCompetitionIdAndPublicVisibleTrueOrderByDisplayOrderAscEventCodeAsc(competition.getId())
                .stream()
                .filter(event -> Boolean.TRUE.equals(event.getScoreVisible()))
                .filter(event -> event.getStatus() != EventStatus.DRAFT)
                .toList();
        List<CompetitionCategory> categories = categoryRepository
                .findByCompetitionIdAndActiveTrueOrderByDisplayOrderAscNameAsc(competition.getId());
        return buildLeaderboard(
                competition,
                events,
                categories,
                EventRankingService.PUBLIC_SCORE_STATUSES
        );
    }

    public AthleteLeaderboardPositionResponse getAthletePosition(Long competitionId, Long userAccountId) {
        Competition competition = findCompetition(competitionId);
        CompetitionAthlete athlete = athleteRepository
                .findByCompetitionIdAndUserAccountId(competitionId, userAccountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Athlete profile not found for this competition."));
        OverallLeaderboardResponse leaderboard = getPublicLeaderboard(competition.getSlug());

        OverallLeaderboardRow row = leaderboard.categories().stream()
                .filter(category -> category.categoryId().equals(athlete.getCategory().getId()))
                .flatMap(category -> category.rows().stream())
                .filter(candidate -> candidate.athleteId().equals(athlete.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Athlete is not eligible for this leaderboard."));

        return new AthleteLeaderboardPositionResponse(
                leaderboard.competitionId(),
                leaderboard.competitionName(),
                row.categoryId(),
                row.categoryName(),
                row.athleteId(),
                row.athleteName(),
                row.rank(),
                row.tied(),
                row.totalPoints(),
                row.scoredEvents(),
                row.totalEvents(),
                row.eventWins(),
                row.topThreePlacements(),
                leaderboard.status(),
                leaderboard.lastUpdatedAt(),
                row.eventResults()
        );
    }

    private OverallLeaderboardResponse buildLeaderboard(
            Competition competition,
            List<CompetitionEvent> events,
            List<CompetitionCategory> categories,
            Set<ScoreStatus> includedStatuses
    ) {
        List<CompetitionAthlete> athletes = athleteRepository
                .findByCompetitionIdAndStatusNotInOrderByFullNameAsc(
                        competition.getId(),
                        List.of(AthleteStatus.WITHDRAWN, AthleteStatus.DISQUALIFIED)
                );

        Set<Long> includedEventIds = events.stream()
                .map(CompetitionEvent::getId)
                .collect(Collectors.toSet());
        List<CompetitionScore> scores = scoreRepository
                .findByEventCompetitionIdAndStatusIn(competition.getId(), includedStatuses)
                .stream()
                .filter(score -> includedEventIds.contains(score.getEvent().getId()))
                .toList();

        Map<Long, List<CompetitionScore>> scoresByEvent = scores.stream()
                .collect(Collectors.groupingBy(score -> score.getEvent().getId()));

        List<OverallLeaderboardResponse.CategoryLeaderboard> categoryBoards = categories.stream()
                .map(category -> buildCategoryBoard(
                        category,
                        events,
                        athletes,
                        scoresByEvent
                ))
                .toList();

        List<OverallLeaderboardResponse.EventSummary> eventSummaries = events.stream()
                .map(event -> new OverallLeaderboardResponse.EventSummary(
                        event.getId(),
                        event.getEventCode(),
                        event.getName(),
                        event.getScoreType(),
                        event.getWeightUnit(),
                        event.getDisplayOrder()
                ))
                .toList();

        return new OverallLeaderboardResponse(
                competition.getId(),
                competition.getName(),
                competition.getSlug(),
                eventRankingService.leaderboardStatus(scores),
                latestUpdate(competition, events, scores),
                eventSummaries,
                categoryBoards
        );
    }

    private OverallLeaderboardResponse.CategoryLeaderboard buildCategoryBoard(
            CompetitionCategory category,
            List<CompetitionEvent> events,
            List<CompetitionAthlete> competitionAthletes,
            Map<Long, List<CompetitionScore>> scoresByEvent
    ) {
        List<CompetitionAthlete> categoryAthletes = competitionAthletes.stream()
                .filter(athlete -> category.getId().equals(athlete.getCategory().getId()))
                .toList();

        Map<Long, Map<Long, EventLeaderboardRow>> eventRowsByAthlete = new LinkedHashMap<>();
        for (CompetitionEvent event : events) {
            Map<Long, EventLeaderboardRow> rowsByAthlete = eventRankingService
                    .rankForCategory(
                            event,
                            category,
                            categoryAthletes,
                            scoresByEvent.getOrDefault(event.getId(), List.of())
                    )
                    .stream()
                    .collect(Collectors.toMap(
                            EventLeaderboardRow::athleteId,
                            Function.identity(),
                            (first, ignored) -> first,
                            LinkedHashMap::new
                    ));
            eventRowsByAthlete.put(event.getId(), rowsByAthlete);
        }

        List<OverallCandidate> candidates = categoryAthletes.stream()
                .map(athlete -> candidateFor(athlete, events, eventRowsByAthlete))
                .toList();

        List<OverallLeaderboardRow> rankedRows = rankOverallCandidates(candidates);
        return new OverallLeaderboardResponse.CategoryLeaderboard(
                category.getId(),
                category.getName(),
                category.getGenderClassification(),
                category.getDivisionLabel(),
                category.getActive(),
                rankedRows
        );
    }

    private OverallCandidate candidateFor(
            CompetitionAthlete athlete,
            List<CompetitionEvent> events,
            Map<Long, Map<Long, EventLeaderboardRow>> eventRowsByAthlete
    ) {
        List<EventLeaderboardRow> eventResults = events.stream()
                .map(event -> eventRowsByAthlete.getOrDefault(event.getId(), Map.of()).get(athlete.getId()))
                .filter(result -> result != null)
                .toList();
        int totalEligibleEvents = (int) eventRowsByAthlete.values().stream()
                .filter(rowsByAthlete -> rowsByAthlete.containsKey(athlete.getId()))
                .count();

        List<EventLeaderboardRow> scoredResults = eventResults.stream()
                .filter(result -> result.placementPoints() != null)
                .toList();
        int scoredEvents = scoredResults.size();
        Integer totalPoints = scoredEvents == 0
                ? null
                : scoredResults.stream().mapToInt(EventLeaderboardRow::placementPoints).sum();
        int eventWins = (int) scoredResults.stream()
                .filter(result -> Integer.valueOf(1).equals(result.rank()))
                .count();
        int topThreePlacements = (int) scoredResults.stream()
                .filter(result -> result.rank() != null && result.rank() <= 3)
                .count();
        Integer mostRecentEventPlacement = eventResults.isEmpty()
                ? null
                : eventResults.get(eventResults.size() - 1).rank();

        return new OverallCandidate(
                athlete,
                totalPoints,
                scoredEvents,
                eventWins,
                topThreePlacements,
                mostRecentEventPlacement,
                totalEligibleEvents,
                eventResults
        );
    }

    private List<OverallLeaderboardRow> rankOverallCandidates(List<OverallCandidate> candidates) {
        List<OverallCandidate> scoredCandidates = candidates.stream()
                .filter(candidate -> candidate.scoredEvents() > 0)
                .sorted(overallOrder())
                .toList();
        List<OverallCandidate> unscoredCandidates = candidates.stream()
                .filter(candidate -> candidate.scoredEvents() == 0)
                .sorted(candidateNameOrder())
                .toList();

        Map<Long, Integer> rankByAthleteId = new HashMap<>();
        Map<Integer, Integer> rankFrequency = new HashMap<>();
        OverallCandidate previous = null;
        int currentRank = 0;
        for (int index = 0; index < scoredCandidates.size(); index++) {
            OverallCandidate candidate = scoredCandidates.get(index);
            if (previous == null || compareCompetitive(previous, candidate) != 0) {
                currentRank = index + 1;
            }
            rankByAthleteId.put(candidate.athlete().getId(), currentRank);
            rankFrequency.merge(currentRank, 1, Integer::sum);
            previous = candidate;
        }

        List<OverallLeaderboardRow> rows = new ArrayList<>();
        for (OverallCandidate candidate : scoredCandidates) {
            int rank = rankByAthleteId.get(candidate.athlete().getId());
            rows.add(toOverallRow(
                    candidate,
                    rank,
                    rankFrequency.getOrDefault(rank, 0) > 1,
                    candidate.totalEligibleEvents()
            ));
        }
        for (OverallCandidate candidate : unscoredCandidates) {
            rows.add(toOverallRow(candidate, null, false, candidate.totalEligibleEvents()));
        }
        return List.copyOf(rows);
    }

    private OverallLeaderboardRow toOverallRow(
            OverallCandidate candidate,
            Integer rank,
            boolean tied,
            int totalEvents
    ) {
        CompetitionAthlete athlete = candidate.athlete();
        return new OverallLeaderboardRow(
                rank,
                tied,
                athlete.getId(),
                athlete.getFullName(),
                athlete.getBibNumber(),
                athlete.getCountry(),
                athlete.getGymName(),
                athlete.getCategory().getId(),
                athlete.getCategory().getName(),
                candidate.totalPoints(),
                candidate.scoredEvents(),
                totalEvents,
                candidate.eventWins(),
                candidate.topThreePlacements(),
                candidate.mostRecentEventPlacement(),
                candidate.eventResults()
        );
    }

    private Comparator<OverallCandidate> overallOrder() {
        return (left, right) -> {
            int competitive = compareCompetitive(left, right);
            if (competitive != 0) {
                return competitive;
            }
            return compareCandidateNames(left, right);
        };
    }

    private Comparator<OverallCandidate> candidateNameOrder() {
        return this::compareCandidateNames;
    }

    private int compareCompetitive(OverallCandidate left, OverallCandidate right) {
        int comparison = Integer.compare(
                right.scoredEvents(),
                left.scoredEvents()
        );
        if (comparison != 0) {
            return comparison;
        }

        comparison = Comparator.nullsLast(Integer::compareTo)
                .compare(left.totalPoints(), right.totalPoints());
        if (comparison != 0) {
            return comparison;
        }

        return comparePlacementsBestToWorst(left, right);
    }

    private int comparePlacementsBestToWorst(
            OverallCandidate left,
            OverallCandidate right
    ) {
        return compareSortedPlacements(
                placementsBestToWorst(left),
                placementsBestToWorst(right)
        );
    }

    static int compareSortedPlacements(
            List<Integer> leftPlacements,
            List<Integer> rightPlacements
    ) {
        int sharedPlacements = Math.min(
                leftPlacements.size(),
                rightPlacements.size()
        );

        for (int index = 0; index < sharedPlacements; index++) {
            int comparison = Integer.compare(
                    leftPlacements.get(index),
                    rightPlacements.get(index)
            );

            if (comparison != 0) {
                return comparison;
            }
        }

        return Integer.compare(
                rightPlacements.size(),
                leftPlacements.size()
        );
    }

    private List<Integer> placementsBestToWorst(OverallCandidate candidate) {
        return candidate.eventResults().stream()
                .map(EventLeaderboardRow::rank)
                .filter(rank -> rank != null)
                .sorted()
                .toList();
    }

    private int compareCandidateNames(OverallCandidate left, OverallCandidate right) {
        int nameComparison = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                .compare(left.athlete().getFullName(), right.athlete().getFullName());
        if (nameComparison != 0) {
            return nameComparison;
        }
        return Comparator.nullsLast(Long::compareTo)
                .compare(left.athlete().getId(), right.athlete().getId());
    }

    private Instant latestUpdate(
            Competition competition,
            List<CompetitionEvent> events,
            List<CompetitionScore> scores
    ) {
        Instant latest = competition.getUpdatedAt();
        for (CompetitionEvent event : events) {
            if (event.getUpdatedAt() != null && (latest == null || event.getUpdatedAt().isAfter(latest))) {
                latest = event.getUpdatedAt();
            }
        }
        for (CompetitionScore score : scores) {
            if (score.getUpdatedAt() != null && (latest == null || score.getUpdatedAt().isAfter(latest))) {
                latest = score.getUpdatedAt();
            }
        }
        return latest;
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

    private record OverallCandidate(
            CompetitionAthlete athlete,
            Integer totalPoints,
            int scoredEvents,
            int eventWins,
            int topThreePlacements,
            Integer mostRecentEventPlacement,
            int totalEligibleEvents,
            List<EventLeaderboardRow> eventResults
    ) {
    }
}
