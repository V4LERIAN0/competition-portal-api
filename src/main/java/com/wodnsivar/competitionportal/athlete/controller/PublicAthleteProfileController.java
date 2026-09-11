package com.wodnsivar.competitionportal.athlete.controller;

import com.wodnsivar.competitionportal.athlete.service.AthleteExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PublicAthleteProfileController {
  private final AthleteExperienceService athletes;

  @GetMapping("/api/public/competitions/{slug}/athletes/{athleteId}")
  public AthleteExperienceService.PublicProfile profile(
      @PathVariable String slug, @PathVariable Long athleteId) {
    return athletes.profile(slug, athleteId);
  }
}
