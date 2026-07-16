package com.wodnsivar.competitionportal.competition.service;

import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.common.exception.ResourceNotFoundException;
import com.wodnsivar.competitionportal.common.util.SlugUtils;
import com.wodnsivar.competitionportal.competition.dto.CompetitionCreateRequest;
import com.wodnsivar.competitionportal.competition.dto.CompetitionResponse;
import com.wodnsivar.competitionportal.competition.dto.CompetitionSummaryResponse;
import com.wodnsivar.competitionportal.competition.dto.CompetitionUpdateRequest;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.entity.CompetitionLandingContent;
import com.wodnsivar.competitionportal.competition.repository.CompetitionLandingContentRepository;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.RegistrationStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionLandingContentRepository landingContentRepository;

    public CompetitionResponse createCompetition(CompetitionCreateRequest request) {
        String slug = resolveUniqueSlug(request.slug(), request.name(), null);

        Competition competition = Competition.builder()
                .name(request.name())
                .slug(slug)
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .locationName(request.locationName())
                .address(request.address())
                .eventDate(request.eventDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .registrationStatus(defaultIfNull(request.registrationStatus(), RegistrationStatus.CLOSED))
                .visibilityStatus(defaultIfNull(request.visibilityStatus(), VisibilityStatus.PRIVATE))
                .status(defaultIfNull(request.status(), CompetitionStatus.DRAFT))
                .logoImageUrl(request.logoImageUrl())
                .bannerImageUrl(request.bannerImageUrl())
                .checkInOpenMinutesBeforeHeat(defaultIfNull(request.checkInOpenMinutesBeforeHeat(), 10))
                .build();

        Competition savedCompetition = competitionRepository.save(competition);
        createDefaultLandingContent(savedCompetition);

        return toResponse(savedCompetition);
    }

    @Transactional(readOnly = true)
    public List<CompetitionSummaryResponse> getAllCompetitions() {
        return competitionRepository.findAll()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompetitionResponse getCompetitionById(Long id) {
        Competition competition = findCompetitionOrThrow(id);
        return toResponse(competition);
    }

    @Transactional(readOnly = true)
    public CompetitionResponse getCompetitionBySlug(String slug) {
        Competition competition = competitionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with slug: " + slug));

        if (competition.getVisibilityStatus() != VisibilityStatus.PUBLIC ||
                competition.getStatus() == CompetitionStatus.DRAFT ||
                competition.getStatus() == CompetitionStatus.ARCHIVED) {
            throw new ResourceNotFoundException("Competition not publicly available with slug: " + slug);
        }

        return toResponse(competition);
    }

    public CompetitionResponse updateCompetition(Long id, CompetitionUpdateRequest request) {
        Competition competition = findCompetitionOrThrow(id);

        String slug = resolveUniqueSlug(request.slug(), request.name(), competition.getId());

        competition.setName(request.name());
        competition.setSlug(slug);
        competition.setShortDescription(request.shortDescription());
        competition.setFullDescription(request.fullDescription());
        competition.setLocationName(request.locationName());
        competition.setAddress(request.address());
        competition.setEventDate(request.eventDate());
        competition.setStartTime(request.startTime());
        competition.setEndTime(request.endTime());
        competition.setRegistrationStatus(defaultIfNull(request.registrationStatus(), competition.getRegistrationStatus()));
        competition.setVisibilityStatus(defaultIfNull(request.visibilityStatus(), competition.getVisibilityStatus()));
        competition.setStatus(defaultIfNull(request.status(), competition.getStatus()));
        competition.setLogoImageUrl(request.logoImageUrl());
        competition.setBannerImageUrl(request.bannerImageUrl());
        competition.setCheckInOpenMinutesBeforeHeat(
                defaultIfNull(request.checkInOpenMinutesBeforeHeat(), competition.getCheckInOpenMinutesBeforeHeat())
        );

        Competition savedCompetition = competitionRepository.save(competition);
        return toResponse(savedCompetition);
    }

    public void archiveCompetition(Long id) {
        Competition competition = findCompetitionOrThrow(id);
        competition.setStatus(CompetitionStatus.ARCHIVED);
        competition.setVisibilityStatus(VisibilityStatus.PRIVATE);
        competitionRepository.save(competition);
    }

    private Competition findCompetitionOrThrow(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with id: " + id));
    }

    private void createDefaultLandingContent(Competition competition) {
        CompetitionLandingContent landingContent = CompetitionLandingContent.builder()
                .competition(competition)
                .heroTitle(competition.getName())
                .heroSubtitle("Competition portal")
                .mainDescription(competition.getShortDescription())
                .registrationText("")
                .importantNotes("")
                .contactEmail("")
                .instagramUrl("")
                .facebookUrl("")
                .showAthletes(true)
                .showEvents(true)
                .showHeats(true)
                .showLeaderboard(true)
                .showSponsors(true)
                .build();

        landingContentRepository.save(landingContent);
    }

    private String resolveUniqueSlug(String requestedSlug, String name, Long currentCompetitionId) {
        String baseSlug = requestedSlug != null && !requestedSlug.isBlank()
                ? SlugUtils.toSlug(requestedSlug)
                : SlugUtils.toSlug(name);

        if (baseSlug.isBlank()) {
            throw new ConflictException("Could not generate a valid slug for the competition.");
        }

        Competition existingCompetition = competitionRepository.findBySlug(baseSlug).orElse(null);

        if (existingCompetition == null) {
            return baseSlug;
        }

        if (currentCompetitionId != null && existingCompetition.getId().equals(currentCompetitionId)) {
            return baseSlug;
        }

        throw new ConflictException("A competition with slug '" + baseSlug + "' already exists.");
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private CompetitionResponse toResponse(Competition competition) {
        return new CompetitionResponse(
                competition.getId(),
                competition.getName(),
                competition.getSlug(),
                competition.getShortDescription(),
                competition.getFullDescription(),
                competition.getLocationName(),
                competition.getAddress(),
                competition.getEventDate(),
                competition.getStartTime(),
                competition.getEndTime(),
                competition.getRegistrationStatus(),
                competition.getVisibilityStatus(),
                competition.getStatus(),
                competition.getLogoImageUrl(),
                competition.getBannerImageUrl(),
                competition.getCheckInOpenMinutesBeforeHeat(),
                competition.getCreatedAt(),
                competition.getUpdatedAt()
        );
    }

    private CompetitionSummaryResponse toSummaryResponse(Competition competition) {
        return new CompetitionSummaryResponse(
                competition.getId(),
                competition.getName(),
                competition.getSlug(),
                competition.getShortDescription(),
                competition.getLocationName(),
                competition.getEventDate(),
                competition.getStartTime(),
                competition.getEndTime(),
                competition.getRegistrationStatus(),
                competition.getVisibilityStatus(),
                competition.getStatus(),
                competition.getLogoImageUrl(),
                competition.getBannerImageUrl()
        );
    }
}