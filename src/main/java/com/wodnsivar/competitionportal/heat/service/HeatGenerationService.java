package com.wodnsivar.competitionportal.heat.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.common.exception.BadRequestException;
import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.enums.AthleteStatus;
import com.wodnsivar.competitionportal.enums.CheckInStatus;
import com.wodnsivar.competitionportal.enums.HeatSeedingMode;
import com.wodnsivar.competitionportal.enums.HeatStatus;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.service.EventEligibilityService;
import com.wodnsivar.competitionportal.heat.dto.GenerateHeatsRequest;
import com.wodnsivar.competitionportal.heat.dto.GenerateRandomHeatsRequest;
import com.wodnsivar.competitionportal.heat.dto.HeatCategoryScheduleRequest;
import com.wodnsivar.competitionportal.heat.dto.HeatResponse;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeat;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeatAthlete;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatAthleteRepository;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatRepository;
import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardResponse;
import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardRow;
import com.wodnsivar.competitionportal.leaderboard.dto.OverallLeaderboardResponse;
import com.wodnsivar.competitionportal.leaderboard.dto.OverallLeaderboardRow;
import com.wodnsivar.competitionportal.leaderboard.service.EventRankingService;
import com.wodnsivar.competitionportal.leaderboard.service.OverallLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HeatGenerationService {
    private final HeatService heatService;
    private final CompetitionHeatRepository heatRepository;
    private final CompetitionHeatAthleteRepository assignmentRepository;
    private final CompetitionAthleteRepository athleteRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final EventEligibilityService eventEligibility;
    private final EventRankingService eventRankingService;
    private final OverallLeaderboardService overallLeaderboardService;

    public List<HeatResponse> generate(Long eventId, GenerateHeatsRequest request) {
        CompetitionEvent event = heatService.findEvent(eventId);
        validateSeedingRequest(event, request);

        List<CategorySchedule> categorySchedules = resolveCategorySchedules(event, request.categorySchedules());
        validateCategoriesDoNotAlreadyHaveHeats(eventId, categorySchedules);

        // Cancelled rows created by an older version have no historical value and
        // must not reserve heat names/numbers.
        heatRepository.deleteByEventIdAndStatus(eventId, HeatStatus.CANCELLED);

        Map<Long, CompetitionAthlete> eligibleAthletes = eligibleAthletesById(event);
        Map<Long, Integer> standingByAthleteId = standings(event, request);
        int nextHeatNumber = startingHeatNumber(eventId, request.startingHeatNumber());
        List<CompetitionHeat> generated = new ArrayList<>();

        for (CategorySchedule categorySchedule : categorySchedules) {
            List<CompetitionAthlete> categoryAthletes = eligibleAthletes.values().stream()
                    .filter(athlete -> categorySchedule.category().getId().equals(athlete.getCategory().getId()))
                    .toList();
            if (categoryAthletes.isEmpty()) {
                throw new BadRequestException(
                        "No eligible athletes were found for category " + categorySchedule.category().getName() + ".");
            }

            List<CompetitionAthlete> seededAthletes = seedAthletes(
                    categoryAthletes,
                    categorySchedule.category().getId(),
                    request,
                    standingByAthleteId
            );
            List<List<CompetitionAthlete>> groups = balancedGroups(seededAthletes, request.capacity());

            for (int categoryHeatIndex = 0; categoryHeatIndex < groups.size(); categoryHeatIndex++) {
                CompetitionHeat heat = createHeat(
                        event,
                        categorySchedule,
                        groups.get(categoryHeatIndex),
                        categoryHeatIndex,
                        nextHeatNumber,
                        request
                );
                generated.add(heat);
                nextHeatNumber++;
            }
        }

        return generated.stream().map(heatService::toResponse).toList();
    }

    /**
     * Backwards-compatible endpoint for older admin clients. It now keeps
     * categories isolated and schedules the category blocks sequentially.
     */
    public List<HeatResponse> generateRandom(Long eventId, GenerateRandomHeatsRequest request) {
        CompetitionEvent event = heatService.findEvent(eventId);
        List<CompetitionCategory> categories = request.categoryId() == null
                ? categoryRepository.findByCompetitionIdAndActiveTrueOrderByDisplayOrderAscNameAsc(
                        event.getCompetition().getId())
                : categoryRepository.findAllById(List.of(request.categoryId())).stream().toList();
        if (categories.isEmpty()) {
            throw new BadRequestException("No active categories were selected for heat generation.");
        }

        LocalDateTime categoryStart = request.firstHeatTime();
        List<HeatCategoryScheduleRequest> schedules = new ArrayList<>();
        Map<Long, Long> athleteCounts = activeEligibleAthleteCounts(event);
        int interval = request.minutesBetweenHeats() == null ? 10 : request.minutesBetweenHeats();
        for (CompetitionCategory category : categories.stream().sorted(categoryOrder()).toList()) {
            schedules.add(new HeatCategoryScheduleRequest(category.getId(), categoryStart));
            if (categoryStart != null) {
                long count = athleteCounts.getOrDefault(category.getId(), 0L);
                int heatCount = (int) Math.ceil((double) count / request.capacity());
                categoryStart = categoryStart.plusMinutes((long) heatCount * interval);
            }
        }

        return generate(eventId, new GenerateHeatsRequest(
                HeatSeedingMode.RANDOM,
                schedules,
                request.capacity(),
                request.startingHeatNumber(),
                interval,
                request.publicVisible(),
                request.randomSeed(),
                null
        ));
    }

    private void validateSeedingRequest(CompetitionEvent event, GenerateHeatsRequest request) {
        if (request.seedingMode() == HeatSeedingMode.EVENT_STANDINGS) {
            if (request.sourceEventId() == null) {
                throw new BadRequestException("A source event is required for event-standings seeding.");
            }
            if (request.sourceEventId().equals(event.getId())) {
                throw new BadRequestException("The source event must be different from the event being generated.");
            }
        }
    }

    private List<CategorySchedule> resolveCategorySchedules(
            CompetitionEvent event,
            List<HeatCategoryScheduleRequest> requestedSchedules
    ) {
        Set<Long> categoryIds = new HashSet<>();
        Map<Long, LocalDateTime> firstTimeByCategory = new HashMap<>();
        for (HeatCategoryScheduleRequest schedule : requestedSchedules) {
            if (!categoryIds.add(schedule.categoryId())) {
                throw new BadRequestException("Each category can only be selected once for heat generation.");
            }
            firstTimeByCategory.put(schedule.categoryId(), schedule.firstHeatTime());
        }

        List<CompetitionCategory> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new BadRequestException("One or more selected categories could not be found.");
        }
        for (CompetitionCategory category : categories) {
            if (!event.getCompetition().getId().equals(category.getCompetition().getId())) {
                throw new BadRequestException("Every selected category must belong to the event competition.");
            }
            if (!Boolean.TRUE.equals(category.getActive())) {
                throw new BadRequestException("Inactive categories cannot be used for heat generation.");
            }
        }

        return categories.stream()
                .sorted(categoryOrder())
                .map(category -> new CategorySchedule(category, firstTimeByCategory.get(category.getId())))
                .toList();
    }

    private void validateCategoriesDoNotAlreadyHaveHeats(
            Long eventId,
            List<CategorySchedule> categorySchedules
    ) {
        List<String> existingCategories = categorySchedules.stream()
                .map(CategorySchedule::category)
                .filter(category -> assignmentRepository.existsForEventAndCategoryExcludingHeatStatus(
                        eventId, category.getId(), HeatStatus.CANCELLED))
                .map(CompetitionCategory::getName)
                .toList();
        if (!existingCategories.isEmpty()) {
            throw new ConflictException(
                    "These categories already have heats for this event: " + String.join(", ", existingCategories) + ".");
        }
    }

    private Map<Long, CompetitionAthlete> eligibleAthletesById(CompetitionEvent event) {
        Set<Long> explicitlyEligibleAthleteIds = eventEligibility.explicitlyEligibleAthleteIds(event);
        return athleteRepository.findByCompetitionIdAndStatusNotInOrderByFullNameAsc(
                        event.getCompetition().getId(),
                        List.of(AthleteStatus.WITHDRAWN, AthleteStatus.DISQUALIFIED))
                .stream()
                .filter(athlete -> explicitlyEligibleAthleteIds == null
                        || explicitlyEligibleAthleteIds.contains(athlete.getId()))
                .collect(Collectors.toMap(
                        CompetitionAthlete::getId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, Long> activeEligibleAthleteCounts(CompetitionEvent event) {
        return eligibleAthletesById(event).values().stream()
                .collect(Collectors.groupingBy(
                        athlete -> athlete.getCategory().getId(),
                        Collectors.counting()
                ));
    }

    private Map<Long, Integer> standings(CompetitionEvent event, GenerateHeatsRequest request) {
        if (request.seedingMode() == HeatSeedingMode.RANDOM) {
            return Map.of();
        }
        if (request.seedingMode() == HeatSeedingMode.EVENT_STANDINGS) {
            EventLeaderboardResponse leaderboard = eventRankingService.getAdminPreview(
                    event.getCompetition().getId(), request.sourceEventId());
            return leaderboard.categories().stream()
                    .flatMap(category -> category.rows().stream())
                    .filter(row -> row.rank() != null)
                    .collect(Collectors.toMap(
                            EventLeaderboardRow::athleteId,
                            EventLeaderboardRow::rank,
                            Math::min
                    ));
        }
        OverallLeaderboardResponse leaderboard = overallLeaderboardService.getAdminPreview(
                event.getCompetition().getId());
        return leaderboard.categories().stream()
                .flatMap(category -> category.rows().stream())
                .filter(row -> row.rank() != null)
                .collect(Collectors.toMap(
                        OverallLeaderboardRow::athleteId,
                        OverallLeaderboardRow::rank,
                        Math::min
                ));
    }

    private List<CompetitionAthlete> seedAthletes(
            List<CompetitionAthlete> athletes,
            Long categoryId,
            GenerateHeatsRequest request,
            Map<Long, Integer> standingByAthleteId
    ) {
        List<CompetitionAthlete> seeded = new ArrayList<>(athletes);
        if (request.seedingMode() == HeatSeedingMode.RANDOM) {
            Random random = request.randomSeed() == null
                    ? new Random()
                    : new Random(request.randomSeed() ^ categoryId);
            java.util.Collections.shuffle(seeded, random);
            return seeded;
        }

        // Unranked athletes go first, followed by ranked athletes from weakest
        // to strongest, so the highest-ranked athletes compete in the last heat.
        seeded.sort(Comparator
                .comparing((CompetitionAthlete athlete) -> standingByAthleteId.containsKey(athlete.getId()))
                .thenComparing(
                        athlete -> standingByAthleteId.get(athlete.getId()),
                        Comparator.nullsFirst(Comparator.reverseOrder())
                )
                .thenComparing(CompetitionAthlete::getFullName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CompetitionAthlete::getId));
        return seeded;
    }

    static <T> List<List<T>> balancedGroups(List<T> values, int capacity) {
        int heatCount = (int) Math.ceil((double) values.size() / capacity);
        int minimumSize = values.size() / heatCount;
        int largerHeatCount = values.size() % heatCount;
        int firstLargerHeatIndex = heatCount - largerHeatCount;
        List<List<T>> groups = new ArrayList<>();
        int offset = 0;
        for (int heatIndex = 0; heatIndex < heatCount; heatIndex++) {
            int size = minimumSize + (heatIndex >= firstLargerHeatIndex ? 1 : 0);
            groups.add(List.copyOf(values.subList(offset, offset + size)));
            offset += size;
        }
        return List.copyOf(groups);
    }

    private CompetitionHeat createHeat(
            CompetitionEvent event,
            CategorySchedule categorySchedule,
            List<CompetitionAthlete> athletes,
            int categoryHeatIndex,
            int heatNumber,
            GenerateHeatsRequest request
    ) {
        if (heatRepository.existsByEventIdAndHeatNumber(event.getId(), heatNumber)) {
            throw new ConflictException(
                    "Heat number " + heatNumber + " already exists for this event. Choose a later starting number.");
        }
        LocalDateTime scheduledTime = categorySchedule.firstHeatTime() == null
                ? null
                : categorySchedule.firstHeatTime()
                .plusMinutes((long) categoryHeatIndex * request.minutesBetweenHeats());
        CompetitionHeat heat = CompetitionHeat.builder()
                .competition(event.getCompetition())
                .event(event)
                .name(categorySchedule.category().getName() + " · Heat " + (categoryHeatIndex + 1))
                .heatNumber(heatNumber)
                .scheduledTime(scheduledTime)
                .status(HeatStatus.SCHEDULED)
                .capacity(request.capacity())
                .displayOrder(heatNumber)
                .publicVisible(Boolean.TRUE.equals(request.publicVisible()))
                .build();
        heat = heatRepository.save(heat);

        for (int position = 1; position <= athletes.size(); position++) {
            CompetitionHeatAthlete assignment = CompetitionHeatAthlete.builder()
                    .heat(heat)
                    .athlete(athletes.get(position - 1))
                    .positionNumber(position)
                    .checkInStatus(CheckInStatus.NOT_OPEN)
                    .build();
            heat.getAssignments().add(assignment);
            assignmentRepository.save(assignment);
        }
        return heat;
    }

    private int startingHeatNumber(Long eventId, Integer requestedStart) {
        if (requestedStart != null) {
            return requestedStart;
        }
        return heatRepository.findByEventIdOrderByDisplayOrderAscHeatNumberAsc(eventId)
                .stream()
                .map(CompetitionHeat::getHeatNumber)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private Comparator<CompetitionCategory> categoryOrder() {
        return Comparator
                .comparing(CompetitionCategory::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CompetitionCategory::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CompetitionCategory::getId);
    }

    private record CategorySchedule(
            CompetitionCategory category,
            LocalDateTime firstHeatTime
    ) {
    }
}
