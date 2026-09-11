package com.wodnsivar.competitionportal.announcement.entity;

import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "competition_announcements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionAnnouncement extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competition_id", nullable = false)
  private Competition competition;

  @Column(nullable = false, length = 140)
  private String title;

  @Column(nullable = false, length = 2000)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Audience audience;

  @Column(nullable = false)
  private boolean published;

  public enum Audience {
    PUBLIC,
    ATHLETES,
    JUDGES
  }
}
