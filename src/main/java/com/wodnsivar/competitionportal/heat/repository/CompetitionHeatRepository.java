package com.wodnsivar.competitionportal.heat.repository;

import com.wodnsivar.competitionportal.heat.entity.CompetitionHeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionHeatRepository extends JpaRepository<CompetitionHeat, Long> {
    List<CompetitionHeat> findByEventIdOrderByDisplayOrderAscHeatNumberAsc(Long eventId);
    List<CompetitionHeat> findByCompetitionIdAndPublicVisibleTrueOrderByScheduledTimeAscHeatNumberAsc(Long competitionId);
    boolean existsByEventIdAndHeatNumber(Long eventId, Integer heatNumber);
    boolean existsByEventIdAndHeatNumberAndIdNot(Long eventId, Integer heatNumber, Long id);
    boolean existsByEventId(Long eventId);
}
