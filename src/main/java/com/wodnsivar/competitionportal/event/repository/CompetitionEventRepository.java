package com.wodnsivar.competitionportal.event.repository;

import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionEventRepository extends JpaRepository<CompetitionEvent, Long> {

    List<CompetitionEvent> findByCompetitionIdOrderByDisplayOrderAscEventCodeAsc(Long competitionId);

    List<CompetitionEvent> findByCompetitionIdAndPublicVisibleTrueOrderByDisplayOrderAscEventCodeAsc(Long competitionId);

    Optional<CompetitionEvent> findByCompetitionIdAndEventCodeIgnoreCase(Long competitionId, String eventCode);

    boolean existsByCompetitionIdAndEventCodeIgnoreCase(Long competitionId, String eventCode);
}