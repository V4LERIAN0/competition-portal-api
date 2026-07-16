package com.wodnsivar.competitionportal.competition.repository;

import com.wodnsivar.competitionportal.competition.entity.CompetitionLandingContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionLandingContentRepository extends JpaRepository<CompetitionLandingContent, Long> {

    Optional<CompetitionLandingContent> findByCompetitionId(Long competitionId);
}