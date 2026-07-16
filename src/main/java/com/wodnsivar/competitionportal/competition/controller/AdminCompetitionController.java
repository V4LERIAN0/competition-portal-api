package com.wodnsivar.competitionportal.competition.controller;

import com.wodnsivar.competitionportal.competition.dto.CompetitionCreateRequest;
import com.wodnsivar.competitionportal.competition.dto.CompetitionResponse;
import com.wodnsivar.competitionportal.competition.dto.CompetitionSummaryResponse;
import com.wodnsivar.competitionportal.competition.dto.CompetitionUpdateRequest;
import com.wodnsivar.competitionportal.competition.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompetitionResponse createCompetition(@Valid @RequestBody CompetitionCreateRequest request) {
        return competitionService.createCompetition(request);
    }

    @GetMapping
    public List<CompetitionSummaryResponse> getAllCompetitions() {
        return competitionService.getAllCompetitions();
    }

    @GetMapping("/{competitionId}")
    public CompetitionResponse getCompetitionById(@PathVariable Long competitionId) {
        return competitionService.getCompetitionById(competitionId);
    }

    @PutMapping("/{competitionId}")
    public CompetitionResponse updateCompetition(
            @PathVariable Long competitionId,
            @Valid @RequestBody CompetitionUpdateRequest request
    ) {
        return competitionService.updateCompetition(competitionId, request);
    }

    @DeleteMapping("/{competitionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveCompetition(@PathVariable Long competitionId) {
        competitionService.archiveCompetition(competitionId);
    }
}