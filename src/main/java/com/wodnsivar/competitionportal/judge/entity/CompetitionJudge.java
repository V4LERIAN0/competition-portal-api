package com.wodnsivar.competitionportal.judge.entity;
import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
@Entity
@Table(name="competition_judges", indexes={
 @Index(name="idx_judges_competition",columnList="competition_id"),
 @Index(name="idx_judges_user_account",columnList="user_account_id")},
 uniqueConstraints={
 @UniqueConstraint(name="uk_judge_competition_email",columnNames={"competition_id","email"}),
 @UniqueConstraint(name="uk_judge_user_account",columnNames="user_account_id")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionJudge extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="competition_id",nullable=false)
 private Competition competition;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_account_id",nullable=false,unique=true)
 private UserAccount userAccount;
 @Column(name="full_name",nullable=false,length=180) private String fullName;
 @Column(nullable=false,length=150) private String email;
 @Column(nullable=false) private Boolean active;
 @OneToMany(mappedBy="judge",cascade=CascadeType.ALL,orphanRemoval=true)
 @Builder.Default private List<CompetitionJudgeAssignment> assignments=new ArrayList<>();
}
