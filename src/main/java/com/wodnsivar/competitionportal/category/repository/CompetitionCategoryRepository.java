package com.wodnsivar.competitionportal.category.repository;

import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionCategoryRepository extends JpaRepository<CompetitionCategory, Long> {

    List<CompetitionCategory> findByCompetitionIdOrderByDisplayOrderAscNameAsc(Long competitionId);

    List<CompetitionCategory> findByCompetitionIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long competitionId);

    Optional<CompetitionCategory> findByCompetitionIdAndNameIgnoreCase(Long competitionId, String name);

    boolean existsByCompetitionIdAndNameIgnoreCase(Long competitionId, String name);
}