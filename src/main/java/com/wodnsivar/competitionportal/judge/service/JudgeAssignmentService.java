package com.wodnsivar.competitionportal.judge.service;

import com.wodnsivar.competitionportal.auth.security.SecurityUtils;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.event.service.EventConfigurationResolver;
import com.wodnsivar.competitionportal.heat.entity.*;
import com.wodnsivar.competitionportal.heat.repository.*;
import com.wodnsivar.competitionportal.judge.dto.*;
import com.wodnsivar.competitionportal.judge.entity.*;
import com.wodnsivar.competitionportal.judge.repository.CompetitionJudgeAssignmentRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class JudgeAssignmentService {
  private final CompetitionJudgeAssignmentRepository assignments;
  private final CompetitionHeatAthleteRepository positions;
  private final CompetitionHeatRepository heats;
  private final JudgeService judges;
  private final EventConfigurationResolver eventConfigurations;

  public JudgeAssignmentResponse assign(Long positionId, JudgeAssignmentRequest r) {
    CompetitionHeatAthlete p =
        positions
            .findById(positionId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Heat assignment not found with id: " + positionId));
    CompetitionJudge j = judges.find(r.judgeId());
    CompetitionHeat h = p.getHeat();
    if (!j.getCompetition().getId().equals(h.getCompetition().getId()))
      throw new ConflictException(
          "The judge and heat position must belong to the same competition.");
    if (!Boolean.TRUE.equals(j.getActive())
        || !Boolean.TRUE.equals(j.getUserAccount().getEnabled()))
      throw new ConflictException("Inactive judges cannot receive assignments.");
    if (assignments.existsByHeatAssignmentId(positionId))
      throw new ConflictException("This heat position already has a judge.");
    if (assignments.existsByJudgeIdAndHeatId(j.getId(), h.getId()))
      throw new ConflictException("This judge already covers another position in this heat.");
    CompetitionJudgeAssignment a =
        CompetitionJudgeAssignment.builder().judge(j).heat(h).heatAssignment(p).build();
    return response(assignments.save(a));
  }

  @Transactional(readOnly = true)
  public List<JudgeAssignmentResponse> listHeat(Long heatId) {
    if (!heats.existsById(heatId))
      throw new ResourceNotFoundException("Heat not found with id: " + heatId);
    return assignments.findForHeat(heatId).stream().map(this::response).toList();
  }

  public void remove(Long id) {
    assignments.delete(
        assignments
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Judge assignment not found with id: " + id)));
  }

  @Transactional(readOnly = true)
  public List<JudgeAssignmentResponse> mine() {
    Long userId = SecurityUtils.getCurrentUserIdOrThrow();
    return assignments.findForJudgeUser(userId).stream()
        .filter(
            a ->
                a.getHeat().getStatus()
                    != com.wodnsivar.competitionportal.enums.HeatStatus.CANCELLED)
        .map(
            a -> {
              if (!Boolean.TRUE.equals(a.getJudge().getActive()))
                throw new ForbiddenException("This judge profile is inactive.");
              return response(a);
            })
        .toList();
  }

  private JudgeAssignmentResponse response(CompetitionJudgeAssignment assignment) {
    CompetitionJudge judge = assignment.getJudge();
    CompetitionHeatAthlete position = assignment.getHeatAssignment();
    CompetitionHeat heat = assignment.getHeat();
    var event = heat.getEvent();
    var effectiveConfiguration =
        eventConfigurations.resolve(event, position.getAthlete().getCategory());

    return new JudgeAssignmentResponse(
        assignment.getId(),
        judge.getId(),
        judge.getFullName(),
        judge.getEmail(),
        judge.getActive(),
        heat.getCompetition().getId(),
        event.getId(),
        event.getEventCode(),
        event.getName(),
        event.getScoreType(),
        effectiveConfiguration.timeCapSeconds(),
        effectiveConfiguration.cappedScoringEnabled(),
        event.getWeightUnit(),
        event.getTiebreakType(),
        event.getTiebreakLabel(),
        event.getTiebreakRequired(),
        event.getTiebreakWeightUnit(),
        heat.getId(),
        heat.getName(),
        heat.getHeatNumber(),
        heat.getScheduledTime(),
        position.getId(),
        position.getAthlete().getId(),
        position.getAthlete().getFullName(),
        position.getAthlete().getBibNumber(),
        position.getAthlete().getCategory().getId(),
        position.getAthlete().getCategory().getName(),
        position.getPositionNumber(),
        effectiveConfiguration.totalReps(),
        effectiveConfiguration.repsPerRound(),
        heat.getStatus(),
        position.getCheckInStatus());
  }
}
