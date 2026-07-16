package com.wodnsivar.competitionportal.event.entity;

import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.enums.EventStatus;
import com.wodnsivar.competitionportal.enums.RankingDirection;
import com.wodnsivar.competitionportal.enums.ScoreType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "competition_events",
        indexes = {
                @Index(name = "idx_events_competition", columnList = "competition_id"),
                @Index(name = "idx_events_status", columnList = "status"),
                @Index(name = "idx_events_public_visible", columnList = "public_visible"),
                @Index(name = "idx_events_display_order", columnList = "display_order")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_competition_code",
                        columnNames = {"competition_id", "event_code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * One competition has many scored events/workouts.
     * Example:
     * SIVARFEST 2026
     * - Event 1
     * - Event 2A
     * - Event 2B
     * - Event 3
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(name = "event_code", nullable = false, length = 30)
    private String eventCode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "workout_instructions", columnDefinition = "TEXT")
    private String workoutInstructions;

    @Column(name = "movement_standards", columnDefinition = "TEXT")
    private String movementStandards;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_type", nullable = false, length = 40)
    private ScoreType scoreType;

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_direction", nullable = false, length = 40)
    private RankingDirection rankingDirection;

    @Column(name = "time_cap_seconds")
    private Integer timeCapSeconds;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "public_visible", nullable = false)
    private Boolean publicVisible;

    @Column(name = "score_visible", nullable = false)
    private Boolean scoreVisible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventStatus status;
}