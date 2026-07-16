package com.wodnsivar.competitionportal.athlete.repository;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.enums.AthleteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompetitionAthleteRepository extends JpaRepository<CompetitionAthlete, Long> {

    List<CompetitionAthlete> findByCompetitionIdOrderByFullNameAsc(Long competitionId);

    List<CompetitionAthlete> findByCompetitionIdAndCategoryIdOrderByFullNameAsc(
            Long competitionId,
            Long categoryId
    );

    List<CompetitionAthlete> findByCompetitionIdAndStatusNotInOrderByFullNameAsc(
            Long competitionId,
            Collection<AthleteStatus> excludedStatuses
    );

    Optional<CompetitionAthlete> findByCompetitionIdAndBibNumber(Long competitionId, String bibNumber);

    boolean existsByCompetitionIdAndBibNumber(Long competitionId, String bibNumber);
}