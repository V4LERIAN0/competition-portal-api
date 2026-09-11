package com.wodnsivar.competitionportal.announcement.repository;

import com.wodnsivar.competitionportal.announcement.entity.CompetitionAnnouncement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionAnnouncementRepository
    extends JpaRepository<CompetitionAnnouncement, Long> {
  List<CompetitionAnnouncement> findByCompetitionIdOrderByCreatedAtDesc(Long competitionId);
}
