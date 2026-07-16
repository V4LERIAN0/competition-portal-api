package com.wodnsivar.competitionportal.athlete.service;

import com.wodnsivar.competitionportal.athlete.dto.AthleteAdminResponse;
import com.wodnsivar.competitionportal.athlete.dto.AthleteCreateRequest;
import com.wodnsivar.competitionportal.athlete.dto.AthletePublicResponse;
import com.wodnsivar.competitionportal.athlete.dto.AthleteUpdateRequest;
import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.common.exception.ResourceNotFoundException;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.AthleteStatus;
import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AthleteService {

    private final CompetitionAthleteRepository athleteRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;

    public AthleteAdminResponse createAthlete(Long competitionId, AthleteCreateRequest request) {
        Competition competition = findCompetitionOrThrow(competitionId);
        CompetitionCategory category = findCategoryOrThrow(request.categoryId());

        validateCategoryBelongsToCompetition(category, competitionId);
        validateBibNumberIsUnique(competitionId, request.bibNumber(), null);

        CompetitionAthlete athlete = CompetitionAthlete.builder()
                .competition(competition)
                .category(category)
                .fullName(request.fullName().trim())
                .email(normalizeNullable(request.email()))
                .phoneNumber(normalizeNullable(request.phoneNumber()))
                .country(normalizeNullable(request.country()))
                .gymName(normalizeNullable(request.gymName()))
                .age(request.age())
                .birthdate(request.birthdate())
                .height(request.height())
                .weight(request.weight())
                .profilePhotoUrl(normalizeNullable(request.profilePhotoUrl()))
                .bibNumber(normalizeNullable(request.bibNumber()))
                .status(defaultIfNull(request.status(), AthleteStatus.REGISTERED))
                .checkedIn(defaultIfNull(request.checkedIn(), false))
                .publicBio(request.publicBio())
                .build();

        CompetitionAthlete savedAthlete = athleteRepository.save(athlete);

        return toAdminResponse(savedAthlete);
    }

    @Transactional(readOnly = true)
    public List<AthleteAdminResponse> getAthletesByCompetition(Long competitionId) {
        ensureCompetitionExists(competitionId);

        return athleteRepository.findByCompetitionIdOrderByFullNameAsc(competitionId)
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AthleteAdminResponse getAthleteById(Long athleteId) {
        CompetitionAthlete athlete = findAthleteOrThrow(athleteId);
        return toAdminResponse(athlete);
    }

    public AthleteAdminResponse updateAthlete(Long athleteId, AthleteUpdateRequest request) {
        CompetitionAthlete athlete = findAthleteOrThrow(athleteId);
        CompetitionCategory category = findCategoryOrThrow(request.categoryId());

        Long competitionId = athlete.getCompetition().getId();

        validateCategoryBelongsToCompetition(category, competitionId);
        validateBibNumberIsUnique(competitionId, request.bibNumber(), athlete.getId());

        athlete.setCategory(category);
        athlete.setFullName(request.fullName().trim());
        athlete.setEmail(normalizeNullable(request.email()));
        athlete.setPhoneNumber(normalizeNullable(request.phoneNumber()));
        athlete.setCountry(normalizeNullable(request.country()));
        athlete.setGymName(normalizeNullable(request.gymName()));
        athlete.setAge(request.age());
        athlete.setBirthdate(request.birthdate());
        athlete.setHeight(request.height());
        athlete.setWeight(request.weight());
        athlete.setProfilePhotoUrl(normalizeNullable(request.profilePhotoUrl()));
        athlete.setBibNumber(normalizeNullable(request.bibNumber()));
        athlete.setStatus(defaultIfNull(request.status(), athlete.getStatus()));
        athlete.setCheckedIn(defaultIfNull(request.checkedIn(), athlete.getCheckedIn()));
        athlete.setPublicBio(request.publicBio());

        CompetitionAthlete savedAthlete = athleteRepository.save(athlete);

        return toAdminResponse(savedAthlete);
    }

    public void withdrawAthlete(Long athleteId) {
        CompetitionAthlete athlete = findAthleteOrThrow(athleteId);
        athlete.setStatus(AthleteStatus.WITHDRAWN);
        athlete.setCheckedIn(false);
        athleteRepository.save(athlete);
    }

    @Transactional(readOnly = true)
    public List<AthletePublicResponse> getPublicAthletesByCompetitionSlug(String competitionSlug) {
        Competition competition = competitionRepository.findBySlug(competitionSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with slug: " + competitionSlug));

        validateCompetitionIsPublic(competition, competitionSlug);

        Set<AthleteStatus> hiddenStatuses = Set.of(
                AthleteStatus.WITHDRAWN,
                AthleteStatus.DISQUALIFIED
        );

        return athleteRepository
                .findByCompetitionIdAndStatusNotInOrderByFullNameAsc(competition.getId(), hiddenStatuses)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    private Competition findCompetitionOrThrow(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with id: " + competitionId));
    }

    private CompetitionCategory findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private CompetitionAthlete findAthleteOrThrow(Long athleteId) {
        return athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + athleteId));
    }

    private void ensureCompetitionExists(Long competitionId) {
        if (!competitionRepository.existsById(competitionId)) {
            throw new ResourceNotFoundException("Competition not found with id: " + competitionId);
        }
    }

    private void validateCategoryBelongsToCompetition(CompetitionCategory category, Long competitionId) {
        if (!category.getCompetition().getId().equals(competitionId)) {
            throw new ConflictException("The selected category does not belong to this competition.");
        }

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new ConflictException("The selected category is inactive.");
        }
    }

    private void validateBibNumberIsUnique(Long competitionId, String bibNumber, Long currentAthleteId) {
        String normalizedBibNumber = normalizeNullable(bibNumber);

        if (normalizedBibNumber == null) {
            return;
        }

        CompetitionAthlete existingAthlete = athleteRepository
                .findByCompetitionIdAndBibNumber(competitionId, normalizedBibNumber)
                .orElse(null);

        if (existingAthlete == null) {
            return;
        }

        if (currentAthleteId != null && existingAthlete.getId().equals(currentAthleteId)) {
            return;
        }

        throw new ConflictException("Bib number '" + normalizedBibNumber + "' is already assigned in this competition.");
    }

    private void validateCompetitionIsPublic(Competition competition, String slug) {
        boolean isPublic = competition.getVisibilityStatus() == VisibilityStatus.PUBLIC;
        boolean isNotHiddenStatus = competition.getStatus() != CompetitionStatus.DRAFT
                && competition.getStatus() != CompetitionStatus.ARCHIVED;

        if (!isPublic || !isNotHiddenStatus) {
            throw new ResourceNotFoundException("Competition not publicly available with slug: " + slug);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private AthleteAdminResponse toAdminResponse(CompetitionAthlete athlete) {
        return new AthleteAdminResponse(
                athlete.getId(),
                athlete.getCompetition().getId(),
                athlete.getCompetition().getName(),
                athlete.getCategory().getId(),
                athlete.getCategory().getName(),
                athlete.getUserAccount() != null ? athlete.getUserAccount().getId() : null,
                athlete.getFullName(),
                athlete.getEmail(),
                athlete.getPhoneNumber(),
                athlete.getCountry(),
                athlete.getGymName(),
                athlete.getAge(),
                athlete.getBirthdate(),
                athlete.getHeight(),
                athlete.getWeight(),
                athlete.getProfilePhotoUrl(),
                athlete.getBibNumber(),
                athlete.getStatus(),
                athlete.getCheckedIn(),
                athlete.getPublicBio(),
                athlete.getCreatedAt(),
                athlete.getUpdatedAt()
        );
    }

    private AthletePublicResponse toPublicResponse(CompetitionAthlete athlete) {
        return new AthletePublicResponse(
                athlete.getId(),
                athlete.getCompetition().getId(),
                athlete.getCategory().getId(),
                athlete.getCategory().getName(),
                athlete.getFullName(),
                athlete.getCountry(),
                athlete.getGymName(),
                athlete.getHeight(),
                athlete.getWeight(),
                athlete.getProfilePhotoUrl(),
                athlete.getBibNumber(),
                athlete.getStatus(),
                athlete.getPublicBio()
        );
    }
}