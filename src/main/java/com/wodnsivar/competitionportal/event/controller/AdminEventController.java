package com.wodnsivar.competitionportal.event.controller;

import com.wodnsivar.competitionportal.event.dto.EventCreateRequest;
import com.wodnsivar.competitionportal.event.dto.EventResponse;
import com.wodnsivar.competitionportal.event.dto.EventUpdateRequest;
import com.wodnsivar.competitionportal.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @PostMapping("/api/admin/competitions/{competitionId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(
            @PathVariable Long competitionId,
            @Valid @RequestBody EventCreateRequest request
    ) {
        return eventService.createEvent(competitionId, request);
    }

    @GetMapping("/api/admin/competitions/{competitionId}/events")
    public List<EventResponse> getEventsByCompetition(@PathVariable Long competitionId) {
        return eventService.getEventsByCompetition(competitionId);
    }

    @GetMapping("/api/admin/events/{eventId}")
    public EventResponse getEventById(@PathVariable Long eventId) {
        return eventService.getEventById(eventId);
    }

    @PutMapping("/api/admin/events/{eventId}")
    public EventResponse updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return eventService.updateEvent(eventId, request);
    }

    @DeleteMapping("/api/admin/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hideEvent(@PathVariable Long eventId) {
        eventService.hideEvent(eventId);
    }
}