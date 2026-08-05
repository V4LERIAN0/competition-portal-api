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
@RequestMapping("/api/admin/competitions/{competitionId}/leaderboard")
@RequiredArgsConstructor
public class AdminLeaderboardController {

    private final OverallLeaderboardService overallLeaderboardService;
    private final EventRankingService eventRankingService;

    @GetMapping("/preview")
    public OverallLeaderboardResponse getPreview(@PathVariable Long competitionId) {
        return overallLeaderboardService.getAdminPreview(competitionId);
    }

    @GetMapping("/events/{eventId}/preview")
    public EventLeaderboardResponse getEventPreview(
            @PathVariable Long competitionId,
            @PathVariable Long eventId
    ) {
        return eventRankingService.getAdminPreview(competitionId, eventId);
    }
}
