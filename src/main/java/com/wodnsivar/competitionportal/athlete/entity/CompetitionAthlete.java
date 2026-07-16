package com.wodnsivar.competitionportal.athlete.entity;

import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.enums.AthleteStatus;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "competition_athletes",
        indexes = {
                @Index(name = "idx_athletes_competition", columnList = "competition_id"),
                @Index(name = "idx_athletes_category", columnList = "category_id"),
                @Index(name = "idx_athletes_email", columnList = "email"),
                @Index(name = "idx_athletes_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_athlete_competition_bib",
                        columnNames = {"competition_id", "bib_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionAthlete extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * The athlete belongs to one competition.
     * This does NOT mean they are a WodNSivar gym member.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    /*
     * Each athlete belongs to one primary category.
     * Leaderboards will later be calculated by this category.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CompetitionCategory category;

    /*
     * Optional account used only if the athlete has login access.
     * This is nullable because admins may register athletes before creating credentials.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", unique = true)
    private UserAccount userAccount;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(length = 150)
    private String email;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(length = 80)
    private String country;

    @Column(name = "gym_name", length = 150)
    private String gymName;

    private Integer age;

    private LocalDate birthdate;

    @Column(precision = 6, scale = 2)
    private BigDecimal height;

    @Column(precision = 6, scale = 2)
    private BigDecimal weight;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "bib_number", length = 30)
    private String bibNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AthleteStatus status;

    @Column(name = "checked_in", nullable = false)
    private Boolean checkedIn;

    @Column(name = "public_bio", columnDefinition = "TEXT")
    private String publicBio;
}