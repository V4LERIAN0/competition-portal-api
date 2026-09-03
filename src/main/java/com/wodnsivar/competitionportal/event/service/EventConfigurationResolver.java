package com.wodnsivar.competitionportal.event.service;

import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.entity.CompetitionEventCategoryConfig;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventCategoryConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventConfigurationResolver {

    private final CompetitionEventCategoryConfigRepository configurations;

    public EffectiveEventConfiguration resolve(
            CompetitionEvent event,
            CompetitionCategory category
    ) {
        CompetitionEventCategoryConfig categoryConfig = configurations
                .findByEventIdAndCategoryId(event.getId(), category.getId())
                .orElse(null);

        return new EffectiveEventConfiguration(
                value(categoryConfig == null ? null : categoryConfig.getTimeCapSeconds(), event.getTimeCapSeconds()),
                value(categoryConfig == null ? null : categoryConfig.getTotalReps(), event.getTotalReps()),
                value(categoryConfig == null ? null : categoryConfig.getRepsPerRound(), event.getRepsPerRound()),
                value(categoryConfig == null ? null : categoryConfig.getCappedScoringEnabled(), event.getCappedScoringEnabled())
        );
    }

    private <T> T value(T override, T fallback) {
        return override != null ? override : fallback;
    }
}
