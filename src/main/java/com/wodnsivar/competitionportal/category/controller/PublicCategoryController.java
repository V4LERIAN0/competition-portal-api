package com.wodnsivar.competitionportal.category.controller;

import com.wodnsivar.competitionportal.category.dto.CategoryResponse;
import com.wodnsivar.competitionportal.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/competitions/{slug}/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> getPublicCategoriesByCompetitionSlug(@PathVariable String slug) {
        return categoryService.getPublicCategoriesByCompetitionSlug(slug);
    }
}