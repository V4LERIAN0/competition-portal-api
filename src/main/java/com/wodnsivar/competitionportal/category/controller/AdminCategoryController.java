package com.wodnsivar.competitionportal.category.controller;

import com.wodnsivar.competitionportal.category.dto.CategoryCreateRequest;
import com.wodnsivar.competitionportal.category.dto.CategoryResponse;
import com.wodnsivar.competitionportal.category.dto.CategoryUpdateRequest;
import com.wodnsivar.competitionportal.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping("/api/admin/competitions/{competitionId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @PathVariable Long competitionId,
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        return categoryService.createCategory(competitionId, request);
    }

    @GetMapping("/api/admin/competitions/{competitionId}/categories")
    public List<CategoryResponse> getCategoriesByCompetition(@PathVariable Long competitionId) {
        return categoryService.getCategoriesByCompetition(competitionId);
    }

    @GetMapping("/api/admin/categories/{categoryId}")
    public CategoryResponse getCategoryById(@PathVariable Long categoryId) {
        return categoryService.getCategoryById(categoryId);
    }

    @PutMapping("/api/admin/categories/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        return categoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/api/admin/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCategory(@PathVariable Long categoryId) {
        categoryService.deactivateCategory(categoryId);
    }
}