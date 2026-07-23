package com.wodnsivar.competitionportal.heat.repository;

import com.wodnsivar.competitionportal.enums.HeatStatus;
import com.wodnsivar.competitionportal.heat.entity.CompetitionHeatAthlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompetitionHeatAthleteRepository extends JpaRepository<CompetitionHeatAthlete, Long> {
    List<CompetitionHeatAthlete> findByHeatIdOrderByPositionNumberAsc(Long heatId);
    boolean existsByHeatIdAndPositionNumber(Long heatId, Integer positionNumber);
    boolean existsByHeatIdAndPositionNumberAndIdNot(Long heatId, Integer positionNumber, Long id);
    @Query("select count(a) > 0 from CompetitionHeatAthlete a " +
            "where a.heat.event.id = :eventId and a.athlete.id = :athleteId " +
            "and a.heat.status <> :excludedStatus")
    boolean existsForEventAndAthleteExcludingHeatStatus(
            @Param("eventId") Long eventId,
            @Param("athleteId") Long athleteId,
            @Param("excludedStatus") HeatStatus excludedStatus);
}
