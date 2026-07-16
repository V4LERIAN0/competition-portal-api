package com.wodnsivar.competitionportal.event.controller;

import com.wodnsivar.competitionportal.event.dto.EventPublicResponse;
import com.wodnsivar.competitionportal.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/competitions/{slug}/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventPublicResponse> getPublicEventsByCompetitionSlug(@PathVariable String slug) {
        return eventService.getPublicEventsByCompetitionSlug(slug);
    }
}