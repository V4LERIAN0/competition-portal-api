package com.wodnsivar.competitionportal.leaderboard.controller;

import com.wodnsivar.competitionportal.leaderboard.dto.EventLeaderboardResponse;
import com.wodnsivar.competitionportal.leaderboard.dto.OverallLeaderboardResponse;
import com.wodnsivar.competitionportal.leaderboard.service.EventRankingService;
import com.wodnsivar.competitionportal.leaderboard.service.OverallLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/competitions/{slug}/leaderboard")
@RequiredArgsConstructor
public class PublicLeaderboardController {

    private final OverallLeaderboardService overallLeaderboardService;
    private final EventRankingService eventRankingService;

    @GetMapping
    public OverallLeaderboardResponse getOverallLeaderboard(@PathVariable String slug) {
        return overallLeaderboardService.getPublicLeaderboard(slug);
    }

    @GetMapping("/events/{eventId}")
    public EventLeaderboardResponse getEventLeaderboard(
            @PathVariable String slug,
            @PathVariable Long eventId
    ) {
        return eventRankingService.getPublicLeaderboard(slug, eventId);
    }
}
