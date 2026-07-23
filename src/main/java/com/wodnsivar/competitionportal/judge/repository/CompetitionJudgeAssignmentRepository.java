package com.wodnsivar.competitionportal.judge.repository;
import com.wodnsivar.competitionportal.judge.entity.CompetitionJudgeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CompetitionJudgeAssignmentRepository extends JpaRepository<CompetitionJudgeAssignment,Long> {
 @Query("select a from CompetitionJudgeAssignment a where a.heat.id=:heatId order by a.heatAssignment.positionNumber asc")
 List<CompetitionJudgeAssignment> findForHeat(@Param("heatId") Long heatId);
 @Query("select a from CompetitionJudgeAssignment a where a.judge.userAccount.id=:userAccountId " +
        "order by a.heat.scheduledTime asc, a.heat.heatNumber asc")
 List<CompetitionJudgeAssignment> findForJudgeUser(@Param("userAccountId") Long userAccountId);
 boolean existsByHeatAssignmentId(Long heatAssignmentId);
 boolean existsByJudgeIdAndHeatId(Long judgeId,Long heatId);
}
