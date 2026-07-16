package com.wodnsivar.competitionportal.athlete.controller;

import com.wodnsivar.competitionportal.athlete.dto.AthletePublicResponse;
import com.wodnsivar.competitionportal.athlete.service.AthleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/competitions/{slug}/athletes")
@RequiredArgsConstructor
public class PublicAthleteController {

    private final AthleteService athleteService;

    @GetMapping
    public List<AthletePublicResponse> getPublicAthletesByCompetitionSlug(@PathVariable String slug) {
        return athleteService.getPublicAthletesByCompetitionSlug(slug);
    }
}