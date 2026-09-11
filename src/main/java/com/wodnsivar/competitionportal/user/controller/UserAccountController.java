package com.wodnsivar.competitionportal.user.controller;

import com.wodnsivar.competitionportal.user.dto.*;
import com.wodnsivar.competitionportal.user.service.UserAccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class UserAccountController {
  private final UserAccountService accounts;

  @GetMapping("/competitions/{competitionId}/athlete-access")
  public List<AthleteAccessResponse> preview(@PathVariable Long competitionId) {
    return accounts.preview(competitionId);
  }

  @PostMapping("/competitions/{competitionId}/athlete-access")
  public List<AthleteAccessResponse> provision(
      @PathVariable Long competitionId, @Valid @RequestBody AthleteAccessRequest request) {
    return accounts.provision(competitionId, request);
  }

  @PostMapping("/athletes/{athleteId}/reset-access")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void reset(
      @PathVariable Long athleteId, @Valid @RequestBody AthleteAccessRequest request) {
    accounts.reset(athleteId, request);
  }
}
