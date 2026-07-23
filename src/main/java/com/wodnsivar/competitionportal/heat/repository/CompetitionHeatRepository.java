package com.wodnsivar.competitionportal.heat.repository;

import com.wodnsivar.competitionportal.enums.HeatStatus;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionHeatRepository extends JpaRepository<CompetitionHeat, Long> {
    List<CompetitionHeat> findByEventIdOrderByDisplayOrderAscHeatNumberAsc(Long eventId);
    List<CompetitionHeat> findByCompetitionIdAndPublicVisibleTrueAndStatusNotOrderByScheduledTimeAscHeatNumberAsc(
            Long competitionId, HeatStatus excludedStatus);
    boolean existsByEventIdAndHeatNumber(Long eventId, Integer heatNumber);
    boolean existsByEventIdAndHeatNumberAndIdNot(Long eventId, Integer heatNumber, Long id);
    boolean existsByEventIdAndStatusNot(Long eventId, HeatStatus excludedStatus);
    Optional<CompetitionHeat> findFirstByEventIdOrderByHeatNumberDesc(Long eventId);
}
