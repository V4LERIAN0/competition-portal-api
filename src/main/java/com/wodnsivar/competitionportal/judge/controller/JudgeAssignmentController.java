package com.wodnsivar.competitionportal.judge.controller;
import com.wodnsivar.competitionportal.judge.dto.JudgeAssignmentResponse;
import com.wodnsivar.competitionportal.judge.service.JudgeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/judge") @RequiredArgsConstructor
public class JudgeAssignmentController {
 private final JudgeAssignmentService assignments;
 @GetMapping("/assignments") public List<JudgeAssignmentResponse> mine(){return assignments.mine();}
}
