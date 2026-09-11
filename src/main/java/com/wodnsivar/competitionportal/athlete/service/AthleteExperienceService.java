package com.wodnsivar.competitionportal.athlete.service;

import com.wodnsivar.competitionportal.athlete.dto.*;
import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.auth.security.SecurityUtils;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.enums.*;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatAthleteRepository;
import com.wodnsivar.competitionportal.heat.service.HeatCheckInService;
import com.wodnsivar.competitionportal.leaderboard.dto.*;
import com.wodnsivar.competitionportal.leaderboard.service.OverallLeaderboardService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AthleteExperienceService {
  private final CompetitionAthleteRepository athletes;
  private final CompetitionHeatAthleteRepository positions;
  private final AthleteService publicAthletes;
  private final OverallLeaderboardService leaderboards;
  private final HeatCheckInService checkIns;

  public CompetitionAthlete mine() {
    return athletes
        .findByUserAccountId(SecurityUtils.getCurrentUserIdOrThrow())
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "No hay un perfil de atleta vinculado a esta cuenta. Contacta a la"
                        + " organización."));
  }

  public AthleteSelfResponse update(AthleteProfileUpdateRequest request) {
    var a = mine();
    a.setCountry(clean(request.country()));
    a.setGymName(clean(request.gymName()));
    a.setHeight(request.height());
    a.setWeight(request.weight());
    a.setPublicBio(clean(request.publicBio()));
    a.setShowBodyMetrics(request.showBodyMetrics());
    return self(athletes.save(a));
  }

  public void setPhoto(String url) {
    mine().setProfilePhotoUrl(url);
  }

  @Transactional(readOnly = true)
  public AthleteDashboardResponse dashboard() {
    var a = mine();
    var heats =
        positions.findByAthleteIdOrderByHeatScheduledTimeAsc(a.getId()).stream()
            .filter(p -> p.getHeat().getStatus() != HeatStatus.CANCELLED)
            .filter(
                p ->
                    Boolean.TRUE.equals(p.getHeat().getPublicVisible())
                        && Boolean.TRUE.equals(p.getHeat().getEvent().getPublicVisible())
                        && p.getHeat().getEvent().getStatus() != EventStatus.DRAFT)
            .map(
                p ->
                    new AthleteHeatResponse(
                        p.getId(),
                        p.getHeat().getEvent().getId(),
                        p.getHeat().getEvent().getName(),
                        p.getHeat().getEvent().getEventCode(),
                        p.getHeat().getName(),
                        p.getHeat().getHeatNumber(),
                        p.getPositionNumber(),
                        p.getHeat().getScheduledTime(),
                        p.getHeat().getStatus(),
                        checkIns.displayStatus(p),
                        checkIns.opensAt(p),
                        checkIns.canCheckIn(p)))
            .toList();
    AthleteLeaderboardPositionResponse standing = null;
    try {
      standing =
          leaderboards.getAthletePosition(a.getCompetition().getId(), a.getUserAccount().getId());
    } catch (ResourceNotFoundException ignored) {
      /* Hidden competition or ineligible athlete: no published standings. */
    }
    return new AthleteDashboardResponse(
        self(a),
        a.getCompetition().getSlug(),
        a.getCompetition().getName(),
        checkIns.timezone(),
        heats,
        standing);
  }

  @Transactional(readOnly = true)
  public PublicProfile profile(String slug, Long athleteId) {
    var athlete =
        publicAthletes.getPublicAthletesByCompetitionSlug(slug).stream()
            .filter(a -> a.id().equals(athleteId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Atleta no encontrado."));
    var board = leaderboards.getPublicLeaderboard(slug);
    var standing =
        board.categories().stream()
            .flatMap(c -> c.rows().stream())
            .filter(a -> a.athleteId().equals(athleteId))
            .findFirst()
            .orElse(null);
    return new PublicProfile(athlete, standing);
  }

  public record PublicProfile(AthletePublicResponse athlete, OverallLeaderboardRow standing) {}

  private AthleteSelfResponse self(CompetitionAthlete a) {
    return new AthleteSelfResponse(
        a.getId(),
        a.getFullName(),
        a.getUserAccount().getUsername(),
        a.getCategory().getName(),
        a.getCountry(),
        a.getGymName(),
        a.getHeight(),
        a.getWeight(),
        a.getPublicBio(),
        a.getProfilePhotoUrl(),
        a.isShowBodyMetrics());
  }

  private String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
