package com.wodnsivar.competitionportal.athlete.dto;

import com.wodnsivar.competitionportal.leaderboard.dto.AthleteLeaderboardPositionResponse;
import java.util.List;

public record AthleteDashboardResponse(
    AthleteSelfResponse profile,
    String competitionSlug,
    String competitionName,
    String timezone,
    List<AthleteHeatResponse> heats,
    AthleteLeaderboardPositionResponse standings) {}
