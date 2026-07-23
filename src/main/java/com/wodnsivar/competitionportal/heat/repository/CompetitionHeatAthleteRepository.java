package com.wodnsivar.competitionportal.heat.repository;

import com.wodnsivar.competitionportal.heat.entity.CompetitionHeatAthlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompetitionHeatAthleteRepository extends JpaRepository<CompetitionHeatAthlete, Long> {
    List<CompetitionHeatAthlete> findByHeatIdOrderByLaneNumberAsc(Long heatId);
    boolean existsByHeatIdAndLaneNumber(Long heatId, Integer laneNumber);
    boolean existsByHeatIdAndLaneNumberAndIdNot(Long heatId, Integer laneNumber, Long id);
    @Query("select count(a) > 0 from CompetitionHeatAthlete a " +
            "where a.heat.event.id = :eventId and a.athlete.id = :athleteId")
    boolean existsForEventAndAthlete(@Param("eventId") Long eventId,
                                     @Param("athleteId") Long athleteId);
}
