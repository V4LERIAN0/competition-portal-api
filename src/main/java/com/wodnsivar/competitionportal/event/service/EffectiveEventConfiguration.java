package com.wodnsivar.competitionportal.event.service;

public record EffectiveEventConfiguration(
        Integer timeCapSeconds,
        Integer totalReps,
        Integer repsPerRound,
        Boolean cappedScoringEnabled
) {
}
