package com.wodnsivar.competitionportal.athlete.controller;

import com.wodnsivar.competitionportal.athlete.dto.AthleteAdminResponse;
import com.wodnsivar.competitionportal.athlete.dto.AthleteCreateRequest;
import com.wodnsivar.competitionportal.athlete.dto.AthleteUpdateRequest;
import com.wodnsivar.competitionportal.athlete.service.AthleteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminAthleteController {

    private final AthleteService athleteService;

    @PostMapping("/api/admin/competitions/{competitionId}/athletes")
    @ResponseStatus(HttpStatus.CREATED)
    public AthleteAdminResponse createAthlete(
            @PathVariable Long competitionId,
            @Valid @RequestBody AthleteCreateRequest request
    ) {
        return athleteService.createAthlete(competitionId, request);
    }

    @GetMapping("/api/admin/competitions/{competitionId}/athletes")
    public List<AthleteAdminResponse> getAthletesByCompetition(@PathVariable Long competitionId) {
        return athleteService.getAthletesByCompetition(competitionId);
    }

    @GetMapping("/api/admin/athletes/{athleteId}")
    public AthleteAdminResponse getAthleteById(@PathVariable Long athleteId) {
        return athleteService.getAthleteById(athleteId);
    }

    @PutMapping("/api/admin/athletes/{athleteId}")
    public AthleteAdminResponse updateAthlete(
            @PathVariable Long athleteId,
            @Valid @RequestBody AthleteUpdateRequest request
    ) {
        return athleteService.updateAthlete(athleteId, request);
    }

    @DeleteMapping("/api/admin/athletes/{athleteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdrawAthlete(@PathVariable Long athleteId) {
        athleteService.withdrawAthlete(athleteId);
    }
}