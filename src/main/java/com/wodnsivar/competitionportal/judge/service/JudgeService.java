package com.wodnsivar.competitionportal.judge.service;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.UserRole;
import com.wodnsivar.competitionportal.judge.dto.*;
import com.wodnsivar.competitionportal.judge.entity.CompetitionJudge;
import com.wodnsivar.competitionportal.judge.repository.CompetitionJudgeRepository;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import com.wodnsivar.competitionportal.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class JudgeService {
 private final CompetitionJudgeRepository judges;
 private final CompetitionRepository competitions;
 private final UserAccountRepository accounts;
 private final PasswordEncoder encoder;
 public JudgeResponse create(Long competitionId,JudgeCreateRequest r){
  Competition c=competitions.findById(competitionId).orElseThrow(()->new ResourceNotFoundException("Competition not found with id: "+competitionId));
  String email=email(r.email()); validateEmail(competitionId,email,null);
  boolean active=r.active()==null||r.active();
  UserAccount account=accounts.save(UserAccount.builder().email(email).passwordHash(encoder.encode(r.password()))
    .role(UserRole.JUDGE).enabled(active).build());
  return response(judges.save(CompetitionJudge.builder().competition(c).userAccount(account)
    .fullName(r.fullName().trim()).email(email).active(active).build()));
 }
 @Transactional(readOnly=true)
 public List<JudgeResponse> list(Long competitionId){
  if(!competitions.existsById(competitionId))throw new ResourceNotFoundException("Competition not found with id: "+competitionId);
  return judges.findByCompetitionIdOrderByFullNameAsc(competitionId).stream().map(this::response).toList();
 }
 public JudgeResponse update(Long id,JudgeUpdateRequest r){
  CompetitionJudge j=find(id); String email=email(r.email()); validateEmail(j.getCompetition().getId(),email,id);
  UserAccount a=j.getUserAccount();
  if(!a.getEmail().equalsIgnoreCase(email)&&accounts.existsByEmail(email))throw new ConflictException("An account already uses email '"+email+"'.");
  boolean active=r.active()==null?j.getActive():r.active();
  a.setEmail(email);a.setEnabled(active);
  if(r.password()!=null&&!r.password().isBlank())a.setPasswordHash(encoder.encode(r.password()));
  accounts.save(a);j.setFullName(r.fullName().trim());j.setEmail(email);j.setActive(active);
  return response(judges.save(j));
 }
 public void delete(Long id){
  CompetitionJudge j=find(id);UserAccount a=j.getUserAccount();judges.delete(j);judges.flush();accounts.delete(a);
 }
 CompetitionJudge find(Long id){return judges.findById(id).orElseThrow(()->new ResourceNotFoundException("Judge not found with id: "+id));}
 private void validateEmail(Long competitionId,String email,Long id){
  boolean duplicate=id==null?judges.existsByCompetitionIdAndEmailIgnoreCase(competitionId,email):
    judges.existsByCompetitionIdAndEmailIgnoreCaseAndIdNot(competitionId,email,id);
  if(duplicate)throw new ConflictException("A judge already uses email '"+email+"' in this competition.");
  if(id==null&&accounts.existsByEmail(email))throw new ConflictException("An account already uses email '"+email+"'.");
 }
 private JudgeResponse response(CompetitionJudge j){return new JudgeResponse(j.getId(),j.getCompetition().getId(),
   j.getUserAccount().getId(),j.getFullName(),j.getEmail(),j.getActive(),j.getCreatedAt(),j.getUpdatedAt());}
 private String email(String value){return value.trim().toLowerCase();}
}
