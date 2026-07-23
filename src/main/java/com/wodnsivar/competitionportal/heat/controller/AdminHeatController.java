package com.wodnsivar.competitionportal.heat.controller;

import com.wodnsivar.competitionportal.heat.dto.*;
import com.wodnsivar.competitionportal.heat.service.HeatGenerationService;
import com.wodnsivar.competitionportal.heat.service.HeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminHeatController {
    private final HeatService heatService;
    private final HeatGenerationService generationService;

    @PostMapping("/api/admin/events/{eventId}/heats")
    @ResponseStatus(HttpStatus.CREATED)
    public HeatResponse create(@PathVariable Long eventId, @Valid @RequestBody HeatCreateRequest request) {
        return heatService.createHeat(eventId, request);
    }
    @GetMapping("/api/admin/events/{eventId}/heats")
    public List<HeatResponse> list(@PathVariable Long eventId) { return heatService.getEventHeats(eventId); }
    @PostMapping("/api/admin/events/{eventId}/heats/generate-random")
    @ResponseStatus(HttpStatus.CREATED)
    public List<HeatResponse> generate(@PathVariable Long eventId,
                                       @Valid @RequestBody GenerateRandomHeatsRequest request) {
        return generationService.generateRandom(eventId, request);
    }
    @GetMapping("/api/admin/heats/{heatId}")
    public HeatResponse get(@PathVariable Long heatId) { return heatService.getHeat(heatId); }
    @PutMapping("/api/admin/heats/{heatId}")
    public HeatResponse update(@PathVariable Long heatId, @Valid @RequestBody HeatUpdateRequest request) {
        return heatService.updateHeat(heatId, request);
    }
    @DeleteMapping("/api/admin/heats/{heatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long heatId) { heatService.cancelHeat(heatId); }
    @PostMapping("/api/admin/heats/{heatId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public HeatResponse assign(@PathVariable Long heatId, @Valid @RequestBody HeatAssignmentRequest request) {
        return heatService.assignAthlete(heatId, request);
    }
    @PutMapping("/api/admin/heat-assignments/{assignmentId}")
    public HeatResponse updateAssignment(@PathVariable Long assignmentId,
                                         @Valid @RequestBody HeatAssignmentRequest request) {
        return heatService.updateAssignment(assignmentId, request);
    }
    @DeleteMapping("/api/admin/heat-assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAssignment(@PathVariable Long assignmentId) { heatService.removeAssignment(assignmentId); }
}
