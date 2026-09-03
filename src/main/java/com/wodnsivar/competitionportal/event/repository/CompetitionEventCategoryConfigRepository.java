package com.wodnsivar.competitionportal.event.repository;

import com.wodnsivar.competitionportal.event.entity.CompetitionEventCategoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionEventCategoryConfigRepository
        extends JpaRepository<CompetitionEventCategoryConfig, Long> {

    List<CompetitionEventCategoryConfig> findByEventIdOrderByCategoryDisplayOrderAscCategoryNameAsc(Long eventId);

    Optional<CompetitionEventCategoryConfig> findByEventIdAndCategoryId(Long eventId, Long categoryId);

    void deleteByEventId(Long eventId);
}
