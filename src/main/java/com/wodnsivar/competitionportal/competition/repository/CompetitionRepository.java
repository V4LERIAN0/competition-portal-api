package com.wodnsivar.competitionportal.competition.repository;

import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    Optional<Competition> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Competition> findByVisibilityStatusAndStatusNot(
            VisibilityStatus visibilityStatus,
            CompetitionStatus status
    );
}