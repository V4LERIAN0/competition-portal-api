package com.wodnsivar.competitionportal.score.entity;

import com.wodnsivar.competitionportal.enums.ScoreAuditAction;
import com.wodnsivar.competitionportal.enums.ScoreStatus;
import com.wodnsivar.competitionportal.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "competition_score_audit",
        indexes = @Index(name = "idx_score_audit_score", columnList = "score_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScoreAuditEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "score_id", nullable = false)
    private CompetitionScore score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScoreAuditAction action;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 30)
    private UserRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ScoreStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ScoreStatus newStatus;

    @Column(name = "score_snapshot", nullable = false, columnDefinition = "TEXT")
    private String scoreSnapshot;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
