package com.wodnsivar.competitionportal.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AthleteAccessRequest(
    @Size(min = 4, max = 72) String temporaryPassword,
    @NotEmpty @Size(max = 500) List<@Valid Entry> athletes,
    boolean useNameBasedPassword) {
  public AthleteAccessRequest(String temporaryPassword, List<Entry> athletes) {
    this(temporaryPassword, athletes, false);
  }

  public record Entry(
      @NotNull Long athleteId,
      @NotBlank @Pattern(regexp = "[a-z0-9]+(?:[.][a-z0-9]+)+") @Size(max = 100) String username) {}
}
