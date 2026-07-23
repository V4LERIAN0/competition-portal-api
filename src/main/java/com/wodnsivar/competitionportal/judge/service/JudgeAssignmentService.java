package com.wodnsivar.competitionportal.judge.service;
import com.wodnsivar.competitionportal.auth.security.SecurityUtils;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.heat.entity.*;
import com.wodnsivar.competitionportal.heat.repository.*;
import com.wodnsivar.competitionportal.judge.dto.*;
import com.wodnsivar.competitionportal.judge.entity.*;
import com.wodnsivar.competitionportal.judge.repository.CompetitionJudgeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class JudgeAssignmentService {
 private final CompetitionJudgeAssignmentRepository assignments;
 private final CompetitionHeatAthleteRepository positions;
 private final CompetitionHeatRepository heats;
 private final JudgeService judges;
 public JudgeAssignmentResponse assign(Long positionId,JudgeAssignmentRequest r){
  CompetitionHeatAthlete p=positions.findById(positionId).orElseThrow(()->new ResourceNotFoundException("Heat assignment not found with id: "+positionId));
  CompetitionJudge j=judges.find(r.judgeId());CompetitionHeat h=p.getHeat();
  if(!j.getCompetition().getId().equals(h.getCompetition().getId()))throw new ConflictException("The judge and heat position must belong to the same competition.");
  if(!Boolean.TRUE.equals(j.getActive())||!Boolean.TRUE.equals(j.getUserAccount().getEnabled()))throw new ConflictException("Inactive judges cannot receive assignments.");
  if(assignments.existsByHeatAssignmentId(positionId))throw new ConflictException("This heat position already has a judge.");
  if(assignments.existsByJudgeIdAndHeatId(j.getId(),h.getId()))throw new ConflictException("This judge already covers another position in this heat.");
  CompetitionJudgeAssignment a=CompetitionJudgeAssignment.builder().judge(j).heat(h).heatAssignment(p).build();
  return response(assignments.save(a));
 }
 @Transactional(readOnly=true)
 public List<JudgeAssignmentResponse> listHeat(Long heatId){
  if(!heats.existsById(heatId))throw new ResourceNotFoundException("Heat not found with id: "+heatId);
  return assignments.findForHeat(heatId).stream().map(this::response).toList();
 }
 public void remove(Long id){assignments.delete(assignments.findById(id).orElseThrow(()->new ResourceNotFoundException("Judge assignment not found with id: "+id)));}
 @Transactional(readOnly=true)
 public List<JudgeAssignmentResponse> mine(){
  Long userId=SecurityUtils.getCurrentUserIdOrThrow();
  return assignments.findForJudgeUser(userId).stream().map(a->{
   if(!Boolean.TRUE.equals(a.getJudge().getActive()))throw new ForbiddenException("This judge profile is inactive.");
   return response(a);
  }).toList();
 }
 private JudgeAssignmentResponse response(CompetitionJudgeAssignment a){
  CompetitionJudge j=a.getJudge();CompetitionHeatAthlete p=a.getHeatAssignment();CompetitionHeat h=a.getHeat();
  return new JudgeAssignmentResponse(a.getId(),j.getId(),j.getFullName(),j.getEmail(),j.getActive(),
   h.getCompetition().getId(),h.getEvent().getId(),h.getEvent().getEventCode(),h.getEvent().getName(),
   h.getId(),h.getName(),h.getHeatNumber(),h.getScheduledTime(),p.getId(),p.getAthlete().getId(),
   p.getAthlete().getFullName(),p.getAthlete().getBibNumber(),p.getAthlete().getCategory().getId(),
   p.getAthlete().getCategory().getName(),p.getPositionNumber());
 }
}
