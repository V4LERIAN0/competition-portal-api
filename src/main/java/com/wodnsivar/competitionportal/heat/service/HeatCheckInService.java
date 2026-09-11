package com.wodnsivar.competitionportal.heat.service;

import com.wodnsivar.competitionportal.auth.security.SecurityUtils;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.enums.*;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeatAthlete;
import com.wodnsivar.competitionportal.heat.repository.CompetitionHeatAthleteRepository;
import com.wodnsivar.competitionportal.judge.repository.CompetitionJudgeAssignmentRepository;
import java.time.*;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HeatCheckInService {
  private final CompetitionHeatAthleteRepository positions;
  private final CompetitionJudgeAssignmentRepository judges;
  private final Clock competitionClock;

  public String timezone() {
    return competitionClock.getZone().getId();
  }

  public LocalDateTime opensAt(CompetitionHeatAthlete p) {
    var time = p.getHeat().getScheduledTime();
    int minutes =
        p.getHeat().getCompetition().getCheckInOpenMinutesBeforeHeat() == null
            ? 30
            : p.getHeat().getCompetition().getCheckInOpenMinutesBeforeHeat();
    return time == null ? null : time.minusMinutes(minutes);
  }

  public boolean canCheckIn(CompetitionHeatAthlete p) {
    if (isCheckedIn(p)
        || p.getCheckInStatus() == CheckInStatus.NO_SHOW
        || p.getAthlete().getStatus() == AthleteStatus.WITHDRAWN
        || p.getAthlete().getStatus() == AthleteStatus.DISQUALIFIED) return false;
    var heat = p.getHeat();
    if (!Boolean.TRUE.equals(heat.getPublicVisible())
        || !Boolean.TRUE.equals(heat.getEvent().getPublicVisible())
        || heat.getEvent().getStatus() == EventStatus.DRAFT) return false;
    if (!Set.of(HeatStatus.SCHEDULED, HeatStatus.DELAYED, HeatStatus.CHECK_IN_OPEN)
        .contains(heat.getStatus())) return false;
    if (heat.getStatus() == HeatStatus.CHECK_IN_OPEN) return true;
    var opens = opensAt(p);
    var now = LocalDateTime.now(competitionClock);
    return opens != null && !now.isBefore(opens) && now.isBefore(heat.getScheduledTime());
  }

  public CheckInStatus displayStatus(CompetitionHeatAthlete p) {
    if (isCheckedIn(p) || p.getCheckInStatus() == CheckInStatus.NO_SHOW)
      return p.getCheckInStatus();
    if (canCheckIn(p)) return CheckInStatus.OPEN;
    if (p.getHeat().getScheduledTime() != null
        && !LocalDateTime.now(competitionClock).isBefore(p.getHeat().getScheduledTime()))
      return CheckInStatus.MISSED;
    return CheckInStatus.NOT_OPEN;
  }

  public void checkIn(Long positionId) {
    var p =
        positions
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Heat no encontrado."));
    var user = p.getAthlete().getUserAccount();
    if (user == null || !user.getId().equals(SecurityUtils.getCurrentUserIdOrThrow()))
      throw new ForbiddenException("Este heat pertenece a otro atleta.");
    if (isCheckedIn(p)) return;
    if (!canCheckIn(p))
      throw new ConflictException("El check-in no está abierto. Consulta a la organización.");
    p.setCheckInStatus(CheckInStatus.CHECKED_IN);
    p.setCheckInTime(LocalDateTime.now(competitionClock));
  }

  public void judgeCheckIn(Long assignmentId) {
    var assignment =
        judges
            .findOwnedAssignment(assignmentId, SecurityUtils.getCurrentUserIdOrThrow())
            .orElseThrow(
                () -> new ForbiddenException("Solo puedes confirmar a tus atletas asignados."));
    if (!Boolean.TRUE.equals(assignment.getJudge().getActive()))
      throw new ForbiddenException("Juez inactivo.");
    manualCheckIn(assignment.getHeatAssignment().getId());
  }

  public void manualCheckIn(Long positionId) {
    var p =
        positions
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Heat no encontrado."));
    if (p.getAthlete().getStatus() == AthleteStatus.WITHDRAWN
        || p.getAthlete().getStatus() == AthleteStatus.DISQUALIFIED)
      throw new ConflictException("Este atleta ya no está habilitado para competir.");
    if (p.getHeat().getStatus() == HeatStatus.CANCELLED
        || p.getHeat().getStatus() == HeatStatus.COMPLETED)
      throw new ConflictException("Este heat ya no admite check-in.");
    if (!isCheckedIn(p)) {
      p.setCheckInStatus(CheckInStatus.MANUAL_CHECKED_IN);
      p.setCheckInTime(LocalDateTime.now(competitionClock));
    }
  }

  private boolean isCheckedIn(CompetitionHeatAthlete p) {
    return p.getCheckInStatus() == CheckInStatus.CHECKED_IN
        || p.getCheckInStatus() == CheckInStatus.MANUAL_CHECKED_IN;
  }
}
