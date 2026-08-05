package com.wodnsivar.competitionportal.score.repository;

import com.wodnsivar.competitionportal.score.entity.CompetitionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionScoreRepository extends JpaRepository<CompetitionScore, Long> {
    Optional<CompetitionScore> findByEventIdAndAthleteId(Long eventId, Long athleteId);
    List<CompetitionScore> findByEventIdOrderByAthleteFullNameAsc(Long eventId);
}
