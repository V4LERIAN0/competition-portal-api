package com.wodnsivar.competitionportal.heat.entity;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.enums.CheckInStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "competition_heat_athletes", indexes = {
        @Index(name = "idx_heat_assignments_heat", columnList = "heat_id"),
        @Index(name = "idx_heat_assignments_athlete", columnList = "athlete_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_heat_athlete", columnNames = {"heat_id", "athlete_id"}),
        @UniqueConstraint(name = "uk_heat_lane", columnNames = {"heat_id", "lane_number"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionHeatAthlete extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "heat_id", nullable = false)
    private CompetitionHeat heat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private CompetitionAthlete athlete;

    @Column(name = "lane_number", nullable = false)
    private Integer laneNumber;

    @Column(name = "station_number")
    private Integer stationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_status", nullable = false, length = 30)
    private CheckInStatus checkInStatus;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
}
