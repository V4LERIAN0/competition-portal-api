package com.wodnsivar.competitionportal.event.service;

import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.common.exception.BadRequestException;
import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.enums.ScoreType;
import com.wodnsivar.competitionportal.event.dto.EventCategoryConfigRequest;
import com.wodnsivar.competitionportal.event.dto.EventCategoryConfigResponse;
import com.wodnsivar.competitionportal.event.dto.EventVariationPublicResponse;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.entity.CompetitionEventCategoryConfig;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventCategoryConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EventCategoryConfigurationService {

    private final CompetitionEventCategoryConfigRepository configurations;
    private final CompetitionCategoryRepository categories;

    public void replace(CompetitionEvent event, List<EventCategoryConfigRequest> requests) {
        if (requests == null) {
            return;
        }

        Set<Long> categoryIds = new HashSet<>();
        for (EventCategoryConfigRequest request : requests) {
            if (!categoryIds.add(request.categoryId())) {
                throw new BadRequestException("An event can only have one configuration per category.");
            }
        }

        configurations.deleteByEventId(event.getId());
        configurations.flush();

        for (EventCategoryConfigRequest request : requests) {
            CompetitionCategory category = categories.findById(request.categoryId())
                    .orElseThrow(() -> new BadRequestException(
                            "Category not found with id: " + request.categoryId()));
            if (!event.getCompetition().getId().equals(category.getCompetition().getId())) {
                throw new BadRequestException("Every event variation must use a category from the same competition.");
            }

            validate(event, request);
            configurations.save(CompetitionEventCategoryConfig.builder()
                    .event(event)
                    .category(category)
                    .variantLabel(normalize(request.variantLabel()))
                    .description(normalize(request.description()))
                    .workoutInstructions(normalize(request.workoutInstructions()))
                    .movementStandards(normalize(request.movementStandards()))
                    .timeCapSeconds(request.timeCapSeconds())
                    .totalReps(request.totalReps())
                    .repsPerRound(request.repsPerRound())
                    .cappedScoringEnabled(request.cappedScoringEnabled())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<EventCategoryConfigResponse> adminResponses(CompetitionEvent event) {
        return configurations.findByEventIdOrderByCategoryDisplayOrderAscCategoryNameAsc(event.getId())
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventVariationPublicResponse> publicResponses(CompetitionEvent event) {
        return configurations.findByEventIdOrderByCategoryDisplayOrderAscCategoryNameAsc(event.getId())
                .stream()
                .filter(config -> Boolean.TRUE.equals(config.getCategory().getActive()))
                .map(config -> toPublicResponse(event, config))
                .toList();
    }

    private void validate(CompetitionEvent event, EventCategoryConfigRequest request) {
        Integer timeCap = value(request.timeCapSeconds(), event.getTimeCapSeconds());
        Integer totalReps = value(request.totalReps(), event.getTotalReps());
        Integer repsPerRound = value(request.repsPerRound(), event.getRepsPerRound());
        Boolean capped = value(request.cappedScoringEnabled(), event.getCappedScoringEnabled());

        positive(timeCap, "Variation time cap must be greater than zero seconds.");
        positive(totalReps, "Variation total reps must be greater than zero.");
        positive(repsPerRound, "Variation reps per round must be greater than zero.");

        if (repsPerRound != null && event.getScoreType() != ScoreType.AMRAP_REPS
                && event.getScoreType() != ScoreType.EMOM_REPS) {
            throw new ConflictException(
                    "Variation reps per round is only supported for AMRAP_REPS and EMOM_REPS events.");
        }

        if (Boolean.TRUE.equals(capped)) {
            if (event.getScoreType() != ScoreType.FOR_TIME) {
                throw new ConflictException("Capped variation scoring is only supported for FOR_TIME events.");
            }
            if (timeCap == null || totalReps == null) {
                throw new ConflictException(
                        "Capped FOR_TIME variations require both a time cap and total reps.");
            }
        }
    }

    private EventCategoryConfigResponse toAdminResponse(CompetitionEventCategoryConfig config) {
        CompetitionCategory category = config.getCategory();
        return new EventCategoryConfigResponse(
                config.getId(), category.getId(), category.getName(),
                category.getGenderClassification(), category.getDivisionLabel(), category.getDisplayOrder(),
                config.getVariantLabel(), config.getDescription(), config.getWorkoutInstructions(),
                config.getMovementStandards(), config.getTimeCapSeconds(), config.getTotalReps(),
                config.getRepsPerRound(), config.getCappedScoringEnabled()
        );
    }

    private EventVariationPublicResponse toPublicResponse(
            CompetitionEvent event,
            CompetitionEventCategoryConfig config
    ) {
        CompetitionCategory category = config.getCategory();
        return new EventVariationPublicResponse(
                category.getId(), category.getName(), category.getGenderClassification(),
                category.getDivisionLabel(), category.getDisplayOrder(),
                value(config.getVariantLabel(), category.getName()),
                value(config.getDescription(), event.getDescription()),
                value(config.getWorkoutInstructions(), event.getWorkoutInstructions()),
                value(config.getMovementStandards(), event.getMovementStandards()),
                value(config.getTimeCapSeconds(), event.getTimeCapSeconds()),
                value(config.getTotalReps(), event.getTotalReps()),
                value(config.getRepsPerRound(), event.getRepsPerRound()),
                value(config.getCappedScoringEnabled(), event.getCappedScoringEnabled())
        );
    }

    private void positive(Integer value, String message) {
        if (value != null && value <= 0) {
            throw new ConflictException(message);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> T value(T override, T fallback) {
        return override != null ? override : fallback;
    }
}
