package com.wodnsivar.competitionportal.category.service;

import com.wodnsivar.competitionportal.category.dto.CategoryCreateRequest;
import com.wodnsivar.competitionportal.category.dto.CategoryResponse;
import com.wodnsivar.competitionportal.category.dto.CategoryUpdateRequest;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.common.exception.ConflictException;
import com.wodnsivar.competitionportal.common.exception.ResourceNotFoundException;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.CompetitionStatus;
import com.wodnsivar.competitionportal.enums.GenderClassification;
import com.wodnsivar.competitionportal.enums.VisibilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CompetitionCategoryRepository categoryRepository;
    private final CompetitionRepository competitionRepository;

    public CategoryResponse createCategory(Long competitionId, CategoryCreateRequest request) {
        Competition competition = findCompetitionOrThrow(competitionId);

        validateCategoryNameIsUnique(competitionId, request.name(), null);

        CompetitionCategory category = CompetitionCategory.builder()
                .competition(competition)
                .name(request.name().trim())
                .genderClassification(defaultIfNull(request.genderClassification(), GenderClassification.OPEN))
                .divisionLabel(request.divisionLabel())
                .description(request.description())
                .displayOrder(defaultIfNull(request.displayOrder(), 0))
                .active(defaultIfNull(request.active(), true))
                .build();

        CompetitionCategory savedCategory = categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByCompetition(Long competitionId) {
        ensureCompetitionExists(competitionId);

        return categoryRepository.findByCompetitionIdOrderByDisplayOrderAscNameAsc(competitionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getPublicCategoriesByCompetitionSlug(String competitionSlug) {
        Competition competition = competitionRepository.findBySlug(competitionSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with slug: " + competitionSlug));

        validateCompetitionIsPublic(competition, competitionSlug);

        return categoryRepository.findByCompetitionIdAndActiveTrueOrderByDisplayOrderAscNameAsc(competition.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryId) {
        CompetitionCategory category = findCategoryOrThrow(categoryId);
        return toResponse(category);
    }

    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        CompetitionCategory category = findCategoryOrThrow(categoryId);

        validateCategoryNameIsUnique(
                category.getCompetition().getId(),
                request.name(),
                category.getId()
        );

        category.setName(request.name().trim());
        category.setGenderClassification(defaultIfNull(request.genderClassification(), GenderClassification.OPEN));
        category.setDivisionLabel(request.divisionLabel());
        category.setDescription(request.description());
        category.setDisplayOrder(defaultIfNull(request.displayOrder(), category.getDisplayOrder()));
        category.setActive(defaultIfNull(request.active(), category.getActive()));

        CompetitionCategory savedCategory = categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    public void deactivateCategory(Long categoryId) {
        CompetitionCategory category = findCategoryOrThrow(categoryId);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Competition findCompetitionOrThrow(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with id: " + competitionId));
    }

    private CompetitionCategory findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private void ensureCompetitionExists(Long competitionId) {
        if (!competitionRepository.existsById(competitionId)) {
            throw new ResourceNotFoundException("Competition not found with id: " + competitionId);
        }
    }

    private void validateCategoryNameIsUnique(Long competitionId, String name, Long currentCategoryId) {
        CompetitionCategory existingCategory = categoryRepository
                .findByCompetitionIdAndNameIgnoreCase(competitionId, name.trim())
                .orElse(null);

        if (existingCategory == null) {
            return;
        }

        if (currentCategoryId != null && existingCategory.getId().equals(currentCategoryId)) {
            return;
        }

        throw new ConflictException("A category with name '" + name + "' already exists in this competition.");
    }

    private void validateCompetitionIsPublic(Competition competition, String slug) {
        boolean isPublic = competition.getVisibilityStatus() == VisibilityStatus.PUBLIC;
        boolean isNotHiddenStatus = competition.getStatus() != CompetitionStatus.DRAFT
                && competition.getStatus() != CompetitionStatus.ARCHIVED;

        if (!isPublic || !isNotHiddenStatus) {
            throw new ResourceNotFoundException("Competition not publicly available with slug: " + slug);
        }
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private CategoryResponse toResponse(CompetitionCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getCompetition().getId(),
                category.getCompetition().getName(),
                category.getName(),
                category.getGenderClassification(),
                category.getDivisionLabel(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}