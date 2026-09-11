package com.wodnsivar.competitionportal.announcement.service;

import com.wodnsivar.competitionportal.announcement.dto.*;
import com.wodnsivar.competitionportal.announcement.entity.CompetitionAnnouncement;
import com.wodnsivar.competitionportal.announcement.entity.CompetitionAnnouncement.Audience;
import com.wodnsivar.competitionportal.announcement.repository.CompetitionAnnouncementRepository;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.auth.security.SecurityUtils;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.*;
import com.wodnsivar.competitionportal.judge.repository.CompetitionJudgeRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {
  private final CompetitionAnnouncementRepository announcements;
  private final CompetitionRepository competitions;
  private final CompetitionAthleteRepository athletes;
  private final CompetitionJudgeRepository judges;

  @Transactional(readOnly = true)
  public List<AnnouncementResponse> admin(Long competitionId) {
    if (!competitions.existsById(competitionId))
      throw new ResourceNotFoundException("Competencia no encontrada.");
    return announcements.findByCompetitionIdOrderByCreatedAtDesc(competitionId).stream()
        .map(this::response)
        .toList();
  }

  public AnnouncementResponse create(Long competitionId, AnnouncementCreateRequest request) {
    var competition =
        competitions
            .findById(competitionId)
            .orElseThrow(() -> new ResourceNotFoundException("Competencia no encontrada."));
    return response(
        announcements.saveAndFlush(
            CompetitionAnnouncement.builder()
                .competition(competition)
                .title(request.title().trim())
                .message(request.message().trim())
                .audience(request.audience())
                .published(request.published())
                .build()));
  }

  public void hide(Long id) {
    announcements
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Aviso no encontrado."))
        .setPublished(false);
  }

  @Transactional(readOnly = true)
  public List<AnnouncementResponse> publicNotices(String slug) {
    var c =
        competitions
            .findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Competencia no encontrada."));
    if (c.getVisibilityStatus() != VisibilityStatus.PUBLIC
        || c.getStatus() == CompetitionStatus.DRAFT
        || c.getStatus() == CompetitionStatus.ARCHIVED)
      throw new ResourceNotFoundException("Competencia no encontrada.");
    return visible(c.getId(), Audience.PUBLIC);
  }

  @Transactional(readOnly = true)
  public List<AnnouncementResponse> mine(boolean judge) {
    Long user = SecurityUtils.getCurrentUserIdOrThrow();
    Long competitionId;
    if (judge) {
      var j =
          judges
              .findByUserAccountId(user)
              .orElseThrow(() -> new ForbiddenException("Juez no encontrado."));
      if (!Boolean.TRUE.equals(j.getActive())) throw new ForbiddenException("Juez inactivo.");
      competitionId = j.getCompetition().getId();
    } else {
      competitionId =
          athletes
              .findByUserAccountId(user)
              .orElseThrow(() -> new ResourceNotFoundException("Atleta no encontrado."))
              .getCompetition()
              .getId();
    }
    return visible(competitionId, judge ? Audience.JUDGES : Audience.ATHLETES);
  }

  private List<AnnouncementResponse> visible(Long competitionId, Audience audience) {
    return announcements.findByCompetitionIdOrderByCreatedAtDesc(competitionId).stream()
        .filter(CompetitionAnnouncement::isPublished)
        .filter(a -> a.getAudience() == Audience.PUBLIC || a.getAudience() == audience)
        .limit(10)
        .map(this::response)
        .toList();
  }

  private AnnouncementResponse response(CompetitionAnnouncement a) {
    return new AnnouncementResponse(
        a.getId(),
        a.getTitle(),
        a.getMessage(),
        a.getAudience(),
        a.isPublished(),
        a.getUpdatedAt());
  }
}
