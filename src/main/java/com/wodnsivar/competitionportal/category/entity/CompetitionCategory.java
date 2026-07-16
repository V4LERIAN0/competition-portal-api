package com.wodnsivar.competitionportal.category.entity;

import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.enums.GenderClassification;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "competition_categories",
        indexes = {
                @Index(name = "idx_categories_competition", columnList = "competition_id"),
                @Index(name = "idx_categories_active", columnList = "active")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_competition_name",
                        columnNames = {"competition_id", "name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_classification", nullable = false, length = 30)
    private GenderClassification genderClassification;

    @Column(name = "division_label", length = 80)
    private String divisionLabel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean active;
}