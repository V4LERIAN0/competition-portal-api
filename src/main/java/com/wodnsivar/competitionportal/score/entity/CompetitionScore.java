package com.wodnsivar.competitionportal.score.entity;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.enums.ScoreStatus;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "competition_scores",
        uniqueConstraints = @UniqueConstraint(name = "uk_score_event_athlete", columnNames = {"event_id", "athlete_id"}),
        indexes = {
                @Index(name = "idx_scores_event", columnList = "event_id"),
                @Index(name = "idx_scores_athlete", columnList = "athlete_id"),
                @Index(name = "idx_scores_status", columnList = "status")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionScore extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompetitionEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private CompetitionAthlete athlete;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScoreStatus status;

    @Column(name = "completed")
    private Boolean completed;

    @Column(name = "score_seconds")
    private Integer scoreSeconds;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "weight_value", precision = 12, scale = 3)
    private BigDecimal weightValue;

    @Column(name = "points_value", precision = 14, scale = 3)
    private BigDecimal pointsValue;

    @Column(name = "custom_value", precision = 14, scale = 3)
    private BigDecimal customValue;

    @Column(name = "tiebreak_value", precision = 14, scale = 3)
    private BigDecimal tiebreakValue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "last_modified_by_user_id", nullable = false)
    private UserAccount lastModifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_user_id")
    private UserAccount validatedBy;
    private Instant validatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_user_id")
    private UserAccount publishedBy;
    private Instant publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by_user_id")
    private UserAccount lockedBy;
    private Instant lockedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
