package com.wodnsivar.competitionportal.competition.entity;

import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.RegistrationStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "competitions",
        indexes = {
                @Index(name = "idx_competitions_slug", columnList = "slug"),
                @Index(name = "idx_competitions_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "full_description", columnDefinition = "TEXT")
    private String fullDescription;

    @Column(name = "location_name", length = 180)
    private String locationName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 30)
    private RegistrationStatus registrationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", nullable = false, length = 30)
    private VisibilityStatus visibilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompetitionStatus status;

    @Column(name = "logo_image_url", length = 500)
    private String logoImageUrl;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(name = "check_in_open_minutes_before_heat", nullable = false)
    private Integer checkInOpenMinutesBeforeHeat;
}