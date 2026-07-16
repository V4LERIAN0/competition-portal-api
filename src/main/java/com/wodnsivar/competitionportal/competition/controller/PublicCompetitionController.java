package com.wodnsivar.competitionportal.competition.controller;

import com.wodnsivar.competitionportal.competition.dto.CompetitionResponse;
import com.wodnsivar.competitionportal.competition.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/competitions")
@RequiredArgsConstructor
public class PublicCompetitionController {

    private final CompetitionService competitionService;

    @GetMapping("/{slug}")
    public CompetitionResponse getPublicCompetitionBySlug(@PathVariable String slug) {
        return competitionService.getCompetitionBySlug(slug);
    }
}