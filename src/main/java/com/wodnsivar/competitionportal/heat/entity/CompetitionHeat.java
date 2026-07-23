package com.wodnsivar.competitionportal.heat.entity;

import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.enums.HeatStatus;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "competition_heats", indexes = {
        @Index(name = "idx_heats_event", columnList = "event_id"),
        @Index(name = "idx_heats_competition", columnList = "competition_id"),
        @Index(name = "idx_heats_scheduled_time", columnList = "scheduled_time")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_heat_event_number", columnNames = {"event_id", "heat_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionHeat extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompetitionEvent event;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "heat_number", nullable = false)
    private Integer heatNumber;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HeatStatus status;

    @Column(nullable = false)
    private Integer capacity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "public_visible", nullable = false)
    private Boolean publicVisible;

    @OneToMany(mappedBy = "heat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionNumber ASC, id ASC")
    @Builder.Default
    private List<CompetitionHeatAthlete> assignments = new ArrayList<>();
}
