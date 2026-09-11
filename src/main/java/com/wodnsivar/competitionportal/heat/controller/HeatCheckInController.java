package com.wodnsivar.competitionportal.heat.controller;

import com.wodnsivar.competitionportal.heat.service.HeatCheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class HeatCheckInController {
  private final HeatCheckInService checkIns;

  @PostMapping("/api/judge/assignments/{assignmentId}/check-in")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void judge(@PathVariable Long assignmentId) {
    checkIns.judgeCheckIn(assignmentId);
  }

  @PostMapping("/api/admin/heat-assignments/{assignmentId}/check-in")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void admin(@PathVariable Long assignmentId) {
    checkIns.manualCheckIn(assignmentId);
  }
}
