package com.wodnsivar.competitionportal.score.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.auth.security.SecurityUtils;
import com.wodnsivar.competitionportal.auth.security.UserPrincipal;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.enums.*;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import com.wodnsivar.competitionportal.event.service.EventConfigurationResolver;
import com.wodnsivar.competitionportal.event.service.EventEligibilityService;
import com.wodnsivar.competitionportal.judge.entity.CompetitionJudgeAssignment;
import com.wodnsivar.competitionportal.judge.repository.CompetitionJudgeAssignmentRepository;
import com.wodnsivar.competitionportal.score.dto.*;
import com.wodnsivar.competitionportal.score.entity.*;
import com.wodnsivar.competitionportal.score.repository.*;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import com.wodnsivar.competitionportal.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoreService {
    private final CompetitionScoreRepository scores;
    private final ScoreAuditEntryRepository audits;
    private final CompetitionEventRepository events;
    private final CompetitionAthleteRepository athletes;
    private final CompetitionJudgeAssignmentRepository judgeAssignments;
    private final UserAccountRepository users;
    private final ScoreValidationService validation;
    private final EventConfigurationResolver eventConfigurations;
    private final EventEligibilityService eventEligibility;

    public ScoreResponse adminUpsert(Long eventId, Long athleteId, ScoreEntryRequest request) {
        CompetitionEvent event = findEvent(eventId);
        CompetitionAthlete athlete = findAthlete(athleteId);
        ensureSameCompetition(event, athlete);
        return upsert(event, athlete, request, false);
    }

    public ScoreResponse judgeUpsert(Long assignmentId, ScoreEntryRequest request) {
        UserPrincipal principal = SecurityUtils.getCurrentUserOrThrow();
        CompetitionJudgeAssignment assignment = judgeAssignments
                .findOwnedAssignment(assignmentId, principal.getId())
                .orElseThrow(() -> new ForbiddenException("You may only score your own assigned heat positions."));
        if (!Boolean.TRUE.equals(assignment.getJudge().getActive())) {
            throw new ForbiddenException("This judge profile is inactive.");
        }
        return upsert(assignment.getHeat().getEvent(), assignment.getHeatAssignment().getAthlete(), request, true);
    }

    private ScoreResponse upsert(CompetitionEvent event, CompetitionAthlete athlete,
                                 ScoreEntryRequest request, boolean judgeEntry) {
        eventEligibility.requireEligible(event, athlete);
        validation.validate(event, eventConfigurations.resolve(event, athlete.getCategory()), request);
        UserAccount actor = currentUser();
        CompetitionScore score = scores.findByEventIdAndAthleteId(event.getId(), athlete.getId()).orElse(null);
        ScoreStatus previous = score == null ? null : score.getStatus();

        if (score != null && (score.getStatus() == ScoreStatus.PUBLISHED || score.getStatus() == ScoreStatus.LOCKED)) {
            throw new ConflictException("Published or locked scores must be explicitly reopened by an admin before editing.");
        }
        if (judgeEntry && score != null && score.getStatus() == ScoreStatus.VALIDATED) {
            throw new ConflictException("A validated score can no longer be changed by a judge.");
        }

        boolean created = score == null;
        if (created) {
            score = CompetitionScore.builder()
                    .event(event).athlete(athlete)
                    .createdBy(actor).lastModifiedBy(actor)
                    .status(judgeEntry ? ScoreStatus.SUBMITTED : ScoreStatus.DRAFT)
                    .build();
        } else {
            score.setLastModifiedBy(actor);
            score.setStatus(judgeEntry ? ScoreStatus.SUBMITTED : ScoreStatus.DRAFT);
        }

        applyValues(score, request);
        score.setRejectionReason(null);
        score.setValidatedBy(null);
        score.setValidatedAt(null);
        CompetitionScore saved = scores.save(score);
        record(saved, created ? ScoreAuditAction.CREATED : ScoreAuditAction.UPDATED, actor,
                previous, saved.getStatus(), null);
        return responseAfterFlush(saved);
    }

    @Transactional(readOnly = true)
    public List<ScoreResponse> listEvent(Long eventId) {
        if (!events.existsById(eventId)) throw new ResourceNotFoundException("Event not found with id: " + eventId);
        return scores.findByEventIdOrderByAthleteFullNameAsc(eventId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public ScoreResponse get(Long scoreId) {
        return response(findScore(scoreId));
    }

    @Transactional(readOnly = true)
    public ScoreResponse judgeGet(Long assignmentId) {
        UserPrincipal principal = SecurityUtils.getCurrentUserOrThrow();
        CompetitionJudgeAssignment assignment = judgeAssignments
                .findOwnedAssignment(assignmentId, principal.getId())
                .orElseThrow(() -> new ForbiddenException("You may only view scores for your own assignments."));
        return scores.findByEventIdAndAthleteId(
                        assignment.getHeat().getEvent().getId(),
                        assignment.getHeatAssignment().getAthlete().getId())
                .map(this::response)
                .orElseThrow(() -> new ResourceNotFoundException("No score has been recorded for this assignment."));
    }

    public ScoreResponse validate(Long id) {
        return transition(id, ScoreStatus.VALIDATED, ScoreAuditAction.VALIDATED, null,
                List.of(ScoreStatus.DRAFT, ScoreStatus.SUBMITTED, ScoreStatus.REJECTED));
    }

    public ScoreResponse reject(Long id, String reason) {
        CompetitionScore score = transitionEntity(id, ScoreStatus.REJECTED, ScoreAuditAction.REJECTED,
                reason.trim(), List.of(ScoreStatus.DRAFT, ScoreStatus.SUBMITTED, ScoreStatus.VALIDATED));
        score.setRejectionReason(reason.trim());
        return responseAfterFlush(scores.save(score));
    }

    public ScoreResponse publish(Long id) {
        CompetitionScore score = transitionEntity(id, ScoreStatus.PUBLISHED, ScoreAuditAction.PUBLISHED,
                null, List.of(ScoreStatus.VALIDATED));
        score.setPublishedBy(currentUser());
        score.setPublishedAt(Instant.now());
        return responseAfterFlush(scores.save(score));
    }

    public ScoreResponse lock(Long id) {
        CompetitionScore score = transitionEntity(id, ScoreStatus.LOCKED, ScoreAuditAction.LOCKED,
                null, List.of(ScoreStatus.PUBLISHED));
        score.setLockedBy(currentUser());
        score.setLockedAt(Instant.now());
        return responseAfterFlush(scores.save(score));
    }

    public ScoreResponse unlock(Long id, String reason) {
        CompetitionScore score = transitionEntity(id, ScoreStatus.PUBLISHED, ScoreAuditAction.UNLOCKED,
                reason.trim(), List.of(ScoreStatus.LOCKED));
        score.setLockedBy(null);
        score.setLockedAt(null);
        return responseAfterFlush(scores.save(score));
    }

    public ScoreResponse reopen(Long id, String reason) {
        CompetitionScore score = transitionEntity(id, ScoreStatus.DRAFT, ScoreAuditAction.REOPENED,
                reason.trim(), List.of(ScoreStatus.PUBLISHED, ScoreStatus.LOCKED));
        score.setValidatedBy(null);
        score.setValidatedAt(null);
        score.setPublishedBy(null);
        score.setPublishedAt(null);
        score.setLockedBy(null);
        score.setLockedAt(null);
        return responseAfterFlush(scores.save(score));
    }

    @Transactional(readOnly = true)
    public List<ScoreAuditResponse> audit(Long scoreId) {
        if (!scores.existsById(scoreId)) throw new ResourceNotFoundException("Score not found with id: " + scoreId);
        return audits.findByScoreIdOrderByOccurredAtAscIdAsc(scoreId).stream()
                .map(a -> new ScoreAuditResponse(a.getId(), a.getAction(), a.getActorUserId(), a.getActorRole(),
                        a.getPreviousStatus(), a.getNewStatus(), a.getScoreSnapshot(), a.getReason(), a.getOccurredAt()))
                .toList();
    }

    private ScoreResponse transition(Long id, ScoreStatus next, ScoreAuditAction action, String reason,
                                     List<ScoreStatus> allowed) {
        return responseAfterFlush(
                scores.save(transitionEntity(id, next, action, reason, allowed))
        );
    }

    private CompetitionScore transitionEntity(Long id, ScoreStatus next, ScoreAuditAction action,
                                              String reason, List<ScoreStatus> allowed) {
        CompetitionScore score = findScore(id);
        if (!allowed.contains(score.getStatus())) {
            throw new ConflictException("Score cannot move from " + score.getStatus() + " to " + next + ".");
        }
        UserAccount actor = currentUser();
        ScoreStatus previous = score.getStatus();
        score.setStatus(next);
        score.setLastModifiedBy(actor);
        if (next == ScoreStatus.VALIDATED) {
            score.setValidatedBy(actor);
            score.setValidatedAt(Instant.now());
            score.setRejectionReason(null);
        }
        CompetitionScore saved = scores.save(score);
        record(saved, action, actor, previous, next, reason);
        return saved;
    }

    private void applyValues(CompetitionScore score, ScoreEntryRequest r) {
        score.setCompleted(r.completed());
        score.setScoreSeconds(r.scoreSeconds());
        score.setReps(r.reps());
        score.setWeightValue(r.weightValue());
        score.setPointsValue(r.pointsValue());
        score.setCustomValue(r.customValue());
        score.setTiebreakValue(r.tiebreakValue());
        score.setNotes(normalize(r.notes()));
    }

    private void record(CompetitionScore score, ScoreAuditAction action, UserAccount actor,
                        ScoreStatus previous, ScoreStatus next, String reason) {
        audits.save(ScoreAuditEntry.builder()
                .score(score).action(action).actorUserId(actor.getId()).actorRole(actor.getRole())
                .previousStatus(previous).newStatus(next).scoreSnapshot(snapshot(score))
                .reason(reason).occurredAt(Instant.now()).build());
    }

    private String snapshot(CompetitionScore s) {
        return "completed=" + s.getCompleted() + ";scoreSeconds=" + s.getScoreSeconds()
                + ";reps=" + s.getReps() + ";weight=" + s.getWeightValue()
                + ";points=" + s.getPointsValue() + ";custom=" + s.getCustomValue()
                + ";tiebreak=" + s.getTiebreakValue();
    }

    private CompetitionEvent findEvent(Long id) {
        return events.findById(id).orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }

    private CompetitionAthlete findAthlete(Long id) {
        return athletes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + id));
    }

    private CompetitionScore findScore(Long id) {
        return scores.findById(id).orElseThrow(() -> new ResourceNotFoundException("Score not found with id: " + id));
    }

    private UserAccount currentUser() {
        Long id = SecurityUtils.getCurrentUserIdOrThrow();
        return users.findById(id).orElseThrow(() -> new ForbiddenException("Authenticated user no longer exists."));
    }

    private void ensureSameCompetition(CompetitionEvent event, CompetitionAthlete athlete) {
        if (!event.getCompetition().getId().equals(athlete.getCompetition().getId())) {
            throw new BadRequestException("Event and athlete must belong to the same competition.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ScoreResponse responseAfterFlush(CompetitionScore score) {
        scores.flush();
        return response(score);
    }

    private ScoreResponse response(CompetitionScore s) {
        CompetitionEvent e = s.getEvent();
        CompetitionAthlete a = s.getAthlete();
        return new ScoreResponse(s.getId(), e.getCompetition().getId(), e.getId(), e.getEventCode(), e.getName(),
                e.getScoreType(), a.getId(), a.getFullName(), a.getBibNumber(), a.getCategory().getId(),
                a.getCategory().getName(), s.getStatus(), s.getCompleted(), s.getScoreSeconds(), s.getReps(),
                s.getWeightValue(), e.getWeightUnit(), s.getPointsValue(), s.getCustomValue(), s.getTiebreakValue(),
                e.getTiebreakType(), e.getTiebreakLabel(), s.getNotes(), s.getCreatedBy().getId(),
                s.getCreatedBy().getRole(), s.getLastModifiedBy().getId(), s.getLastModifiedBy().getRole(),
                id(s.getValidatedBy()), s.getValidatedAt(), id(s.getPublishedBy()), s.getPublishedAt(),
                id(s.getLockedBy()), s.getLockedAt(), s.getRejectionReason(), s.getCreatedAt(), s.getUpdatedAt());
    }
    private Long id(UserAccount user) { return user == null ? null : user.getId(); }
}
