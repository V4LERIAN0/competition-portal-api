package com.wodnsivar.competitionportal.athlete.controller;

import com.wodnsivar.competitionportal.athlete.dto.*;
import com.wodnsivar.competitionportal.athlete.service.AthleteExperienceService;
import com.wodnsivar.competitionportal.heat.service.HeatCheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/athlete/me")
public class AthleteDashboardController {
  private final AthleteExperienceService athletes;
  private final HeatCheckInService checkIns;

  @GetMapping
  public AthleteDashboardResponse dashboard() {
    return athletes.dashboard();
  }

  @PutMapping("/profile")
  public AthleteSelfResponse update(@Valid @RequestBody AthleteProfileUpdateRequest request) {
    return athletes.update(request);
  }

  @PostMapping("/heats/{assignmentId}/check-in")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void checkIn(@PathVariable Long assignmentId) {
    checkIns.checkIn(assignmentId);
  }
}
