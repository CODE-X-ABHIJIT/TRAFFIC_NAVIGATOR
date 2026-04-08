package com.tccs.controller;

import com.tccs.model.dto.ApiResponse;
import com.tccs.model.dto.IncidentRequest;
import com.tccs.model.dto.IncidentResponse;
import com.tccs.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    /**
     * GET /api/incidents
     * All active incidents.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<IncidentResponse>>> getActiveIncidents() {
        return ResponseEntity.ok(
                ApiResponse.ok(incidentService.getActiveIncidents()));
    }

    /**
     * POST /api/incidents
     * Report a new incident.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<IncidentResponse>> reportIncident(
            @Valid @RequestBody IncidentRequest request) {
        IncidentResponse response = incidentService.reportIncident(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Incident reported successfully", response));
    }

    /**
     * PUT /api/incidents/{id}/resolve
     * Resolve an incident.
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<IncidentResponse>> resolveIncident(
            @PathVariable Long id) {
        IncidentResponse response = incidentService.resolveIncident(id);
        return ResponseEntity.ok(
                ApiResponse.ok("Incident resolved", response));
    }
}