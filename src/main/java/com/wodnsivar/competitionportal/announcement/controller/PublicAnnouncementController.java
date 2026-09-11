package com.wodnsivar.competitionportal.announcement.controller;

import com.wodnsivar.competitionportal.announcement.dto.AnnouncementResponse;
import com.wodnsivar.competitionportal.announcement.service.AnnouncementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PublicAnnouncementController {
  private final AnnouncementService announcements;

  @GetMapping("/api/public/competitions/{slug}/announcements")
  public List<AnnouncementResponse> publicNotices(@PathVariable String slug) {
    return announcements.publicNotices(slug);
  }

  @GetMapping("/api/athlete/me/announcements")
  public List<AnnouncementResponse> athlete() {
    return announcements.mine(false);
  }

  @GetMapping("/api/judge/announcements")
  public List<AnnouncementResponse> judge() {
    return announcements.mine(true);
  }
}
