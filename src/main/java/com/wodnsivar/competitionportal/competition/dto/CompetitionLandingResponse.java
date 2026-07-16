package com.wodnsivar.competitionportal.competition.dto;

public record CompetitionLandingResponse(
        Long id,
        Long competitionId,
        String heroTitle,
        String heroSubtitle,
        String mainDescription,
        String registrationText,
        String importantNotes,
        String contactEmail,
        String instagramUrl,
        String facebookUrl,
        Boolean showAthletes,
        Boolean showEvents,
        Boolean showHeats,
        Boolean showLeaderboard,
        Boolean showSponsors
) {
}