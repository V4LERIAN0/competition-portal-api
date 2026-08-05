package com.wodnsivar.competitionportal.leaderboard.controller;

import com.wodnsivar.competitionportal.auth.security.SecurityUtils;
import com.wodnsivar.competitionportal.leaderboard.dto.AthleteLeaderboardPositionResponse;
import com.wodnsivar.competitionportal.leaderboard.service.OverallLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/athlete/competitions/{competitionId}")
@RequiredArgsConstructor
public class AthleteLeaderboardController {

    private final OverallLeaderboardService overallLeaderboardService;

    @GetMapping("/leaderboard-position")
    public AthleteLeaderboardPositionResponse getLeaderboardPosition(
            @PathVariable Long competitionId
    ) {
        return overallLeaderboardService.getAthletePosition(
                competitionId,
                SecurityUtils.getCurrentUserIdOrThrow()
        );
    }
}
