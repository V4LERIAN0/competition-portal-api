package com.wodnsivar.competitionportal.judge.controller;
import com.wodnsivar.competitionportal.judge.dto.*;
import com.wodnsivar.competitionportal.judge.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor
public class AdminJudgeController {
 private final JudgeService judges;private final JudgeAssignmentService assignments;
 @PostMapping("/api/admin/competitions/{competitionId}/judges") @ResponseStatus(HttpStatus.CREATED)
 public JudgeResponse create(@PathVariable Long competitionId,@Valid @RequestBody JudgeCreateRequest r){return judges.create(competitionId,r);}
 @GetMapping("/api/admin/competitions/{competitionId}/judges")
 public List<JudgeResponse> list(@PathVariable Long competitionId){return judges.list(competitionId);}
 @PutMapping("/api/admin/judges/{judgeId}")
 public JudgeResponse update(@PathVariable Long judgeId,@Valid @RequestBody JudgeUpdateRequest r){return judges.update(judgeId,r);}
 @DeleteMapping("/api/admin/judges/{judgeId}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable Long judgeId){judges.delete(judgeId);}
 @PostMapping("/api/admin/heat-assignments/{positionId}/judge") @ResponseStatus(HttpStatus.CREATED)
 public JudgeAssignmentResponse assign(@PathVariable Long positionId,@Valid @RequestBody JudgeAssignmentRequest r){return assignments.assign(positionId,r);}
 @GetMapping("/api/admin/heats/{heatId}/judge-assignments")
 public List<JudgeAssignmentResponse> listHeat(@PathVariable Long heatId){return assignments.listHeat(heatId);}
 @DeleteMapping("/api/admin/judge-assignments/{assignmentId}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void remove(@PathVariable Long assignmentId){assignments.remove(assignmentId);}
}
