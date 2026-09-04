package com.wodnsivar.competitionportal.event.entity;

import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "competition_event_category_configs",
        indexes = {
                @Index(name = "idx_event_category_config_event", columnList = "event_id"),
                @Index(name = "idx_event_category_config_category", columnList = "category_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_category_config",
                columnNames = {"event_id", "category_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionEventCategoryConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompetitionEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CompetitionCategory category;

    @Column(name = "variant_label", length = 120)
    private String variantLabel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "workout_instructions", columnDefinition = "TEXT")
    private String workoutInstructions;

    @Column(name = "movement_standards", columnDefinition = "TEXT")
    private String movementStandards;

    @Column(name = "time_cap_seconds")
    private Integer timeCapSeconds;

    @Column(name = "total_reps")
    private Integer totalReps;

    @Column(name = "reps_per_round")
    private Integer repsPerRound;

    @Column(name = "capped_scoring_enabled")
    private Boolean cappedScoringEnabled;
}
