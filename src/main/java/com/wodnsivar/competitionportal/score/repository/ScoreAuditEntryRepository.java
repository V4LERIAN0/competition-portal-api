package com.wodnsivar.competitionportal.score.repository;

import com.wodnsivar.competitionportal.score.entity.ScoreAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreAuditEntryRepository extends JpaRepository<ScoreAuditEntry, Long> {
    List<ScoreAuditEntry> findByScoreIdOrderByOccurredAtAscIdAsc(Long scoreId);
}
