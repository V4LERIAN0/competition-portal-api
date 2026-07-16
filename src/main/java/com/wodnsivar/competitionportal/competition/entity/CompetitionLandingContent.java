package com.wodnsivar.competitionportal.competition.entity;

import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "competition_landing_content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionLandingContent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false, unique = true)
    private Competition competition;

    @Column(name = "hero_title", length = 200)
    private String heroTitle;

    @Column(name = "hero_subtitle", length = 300)
    private String heroSubtitle;

    @Column(name = "main_description", columnDefinition = "TEXT")
    private String mainDescription;

    @Column(name = "registration_text", columnDefinition = "TEXT")
    private String registrationText;

    @Column(name = "important_notes", columnDefinition = "TEXT")
    private String importantNotes;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "show_athletes", nullable = false)
    private Boolean showAthletes;

    @Column(name = "show_events", nullable = false)
    private Boolean showEvents;

    @Column(name = "show_heats", nullable = false)
    private Boolean showHeats;

    @Column(name = "show_leaderboard", nullable = false)
    private Boolean showLeaderboard;

    @Column(name = "show_sponsors", nullable = false)
    private Boolean showSponsors;
}