package com.wodnsivar.competitionportal.score.controller;

import com.wodnsivar.competitionportal.score.dto.ScoreEntryRequest;
import com.wodnsivar.competitionportal.score.dto.ScoreResponse;
import com.wodnsivar.competitionportal.score.service.ScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/judge/assignments/{assignmentId}/score")
@RequiredArgsConstructor
public class JudgeScoreController {
    private final ScoreService scores;

    @PutMapping
    public ScoreResponse upsert(@PathVariable Long assignmentId,
                                @Valid @RequestBody ScoreEntryRequest request) {
        return scores.judgeUpsert(assignmentId, request);
    }

    @GetMapping
    public ScoreResponse get(@PathVariable Long assignmentId) {
        return scores.judgeGet(assignmentId);
    }
}
