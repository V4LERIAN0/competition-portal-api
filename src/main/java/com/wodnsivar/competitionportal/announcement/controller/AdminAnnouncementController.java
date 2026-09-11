package com.wodnsivar.competitionportal.announcement.controller;

import com.wodnsivar.competitionportal.announcement.dto.*;
import com.wodnsivar.competitionportal.announcement.service.AnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAnnouncementController {
  private final AnnouncementService announcements;

  @GetMapping("/competitions/{competitionId}/announcements")
  public List<AnnouncementResponse> list(@PathVariable Long competitionId) {
    return announcements.admin(competitionId);
  }

  @PostMapping("/competitions/{competitionId}/announcements")
  public AnnouncementResponse create(
      @PathVariable Long competitionId, @Valid @RequestBody AnnouncementCreateRequest request) {
    return announcements.create(competitionId, request);
  }

  @DeleteMapping("/announcements/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void hide(@PathVariable Long id) {
    announcements.hide(id);
  }
}
