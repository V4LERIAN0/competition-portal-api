package com.wodnsivar.competitionportal.event.repository;

import com.wodnsivar.competitionportal.event.entity.CompetitionEventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompetitionEventParticipantRepository
        extends JpaRepository<CompetitionEventParticipant, Long> {

    boolean existsByEventIdAndAthleteId(Long eventId, Long athleteId);

    void deleteByEventId(Long eventId);

    @Query("select participant.athlete.id from CompetitionEventParticipant participant " +
            "where participant.event.id = :eventId order by participant.athlete.fullName asc")
    List<Long> findAthleteIdsByEventId(@Param("eventId") Long eventId);
}
