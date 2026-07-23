package com.wodnsivar.competitionportal.heat.controller;

import com.wodnsivar.competitionportal.heat.dto.HeatResponse;
import com.wodnsivar.competitionportal.heat.service.HeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PublicHeatController {
    private final HeatService heatService;

    @GetMapping("/api/public/competitions/{competitionSlug}/heats")
    public List<HeatResponse> schedule(@PathVariable String competitionSlug) {
        return heatService.getPublicSchedule(competitionSlug);
    }
}
