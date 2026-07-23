package com.wodnsivar.competitionportal.judge.repository;
import com.wodnsivar.competitionportal.judge.entity.CompetitionJudge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CompetitionJudgeRepository extends JpaRepository<CompetitionJudge,Long> {
 List<CompetitionJudge> findByCompetitionIdOrderByFullNameAsc(Long competitionId);
 Optional<CompetitionJudge> findByUserAccountId(Long userAccountId);
 boolean existsByCompetitionIdAndEmailIgnoreCase(Long competitionId,String email);
 boolean existsByCompetitionIdAndEmailIgnoreCaseAndIdNot(Long competitionId,String email,Long id);
}
