package com.wodnsivar.competitionportal.judge.entity;
import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.heat.entity.*;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name="competition_judge_assignments",indexes={
 @Index(name="idx_judge_assignments_judge",columnList="judge_id"),
 @Index(name="idx_judge_assignments_heat",columnList="heat_id")},
 uniqueConstraints={
 @UniqueConstraint(name="uk_judge_assignment_position",columnNames="heat_assignment_id"),
 @UniqueConstraint(name="uk_judge_assignment_judge_heat",columnNames={"judge_id","heat_id"})})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionJudgeAssignment extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="judge_id",nullable=false)
 private CompetitionJudge judge;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="heat_id",nullable=false)
 private CompetitionHeat heat;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="heat_assignment_id",nullable=false,unique=true)
 private CompetitionHeatAthlete heatAssignment;
}
