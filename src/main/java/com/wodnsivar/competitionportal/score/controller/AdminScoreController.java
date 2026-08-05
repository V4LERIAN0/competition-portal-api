package com.wodnsivar.competitionportal.score.controller;

import com.wodnsivar.competitionportal.score.dto.*;
import com.wodnsivar.competitionportal.score.service.ScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminScoreController {
    private final ScoreService scores;

    @PutMapping("/events/{eventId}/athletes/{athleteId}/score")
    public ScoreResponse upsert(@PathVariable Long eventId, @PathVariable Long athleteId,
                                @Valid @RequestBody ScoreEntryRequest request) {
        return scores.adminUpsert(eventId, athleteId, request);
    }

    @GetMapping("/events/{eventId}/scores")
    public List<ScoreResponse> list(@PathVariable Long eventId) {
        return scores.listEvent(eventId);
    }

    @GetMapping("/scores/{scoreId}")
    public ScoreResponse get(@PathVariable Long scoreId) {
        return scores.get(scoreId);
    }

    @PostMapping("/scores/{scoreId}/validate")
    public ScoreResponse validate(@PathVariable Long scoreId) {
        return scores.validate(scoreId);
    }

    @PostMapping("/scores/{scoreId}/reject")
    public ScoreResponse reject(@PathVariable Long scoreId, @Valid @RequestBody ScoreActionRequest request) {
        return scores.reject(scoreId, request.reason());
    }

    @PostMapping("/scores/{scoreId}/publish")
    public ScoreResponse publish(@PathVariable Long scoreId) {
        return scores.publish(scoreId);
    }

    @PostMapping("/scores/{scoreId}/lock")
    public ScoreResponse lock(@PathVariable Long scoreId) {
        return scores.lock(scoreId);
    }

    @PostMapping("/scores/{scoreId}/unlock")
    public ScoreResponse unlock(@PathVariable Long scoreId, @Valid @RequestBody ScoreActionRequest request) {
        return scores.unlock(scoreId, request.reason());
    }

    @PostMapping("/scores/{scoreId}/reopen")
    public ScoreResponse reopen(@PathVariable Long scoreId, @Valid @RequestBody ScoreActionRequest request) {
        return scores.reopen(scoreId, request.reason());
    }

    @GetMapping("/scores/{scoreId}/audit")
    public List<ScoreAuditResponse> audit(@PathVariable Long scoreId) {
        return scores.audit(scoreId);
    }
}
