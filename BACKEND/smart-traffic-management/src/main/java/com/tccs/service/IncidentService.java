package com.tccs.service;

import com.tccs.engine.EmergencyHandler;
import com.tccs.exception.ResourceNotFoundException;
import com.tccs.model.dto.IncidentRequest;
import com.tccs.model.dto.IncidentResponse;
import com.tccs.model.entity.Alert;
import com.tccs.model.entity.Incident;
import com.tccs.model.enums.AlertSeverity;
import com.tccs.model.enums.AlertType;
import com.tccs.repository.AlertRepository;
import com.tccs.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final EmergencyHandler emergencyHandler;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Report a new incident → AUTO EMERGENCY HANDLING.
     */
    @Transactional
    public IncidentResponse reportIncident(IncidentRequest request) {

        // 1. Save incident
        Incident incident = Incident.builder()
                .junctionCode(request.getJunctionCode())
                .incidentType(request.getIncidentType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .severity(request.getSeverity() != null
                        ? request.getSeverity() : AlertSeverity.MEDIUM)
                .reportedBy(request.getReportedBy())
                .active(true)
                .build();

        incident = incidentRepository.save(incident);

        // 2. Generate initial alert
        Alert alert = Alert.builder()
                .junctionCode(request.getJunctionCode())
                .alertType(AlertType.INCIDENT_REPORTED)
                .severity(incident.getSeverity())
                .message(String.format("📋 Incident reported: %s at (%f, %f). %s",
                        request.getIncidentType(),
                        request.getLatitude(), request.getLongitude(),
                        request.getDescription() != null
                                ? request.getDescription() : ""))
                .build();
        alertRepository.save(alert);

        // 3. ━━━ AUTO EMERGENCY HANDLING ━━━
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚨 INCIDENT #{} — {} at ({}, {})",
                incident.getId(), request.getIncidentType(),
                request.getLatitude(), request.getLongitude());
        log.info("  Severity: {} | Reported by: {}",
                incident.getSeverity(),
                request.getReportedBy() != null ? request.getReportedBy() : "System");
        log.info("  Triggering automatic emergency response...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<String> affectedJunctions = emergencyHandler.handleIncident(
                request.getIncidentType(),
                request.getLatitude(),
                request.getLongitude(),
                request.getJunctionCode(),
                incident.getSeverity(),
                incident.getId()
        );

        log.info("🚨 Emergency response complete. {} junctions affected: {}",
                affectedJunctions.size(), affectedJunctions);

        // 4. Broadcast to control room
        messagingTemplate.convertAndSend("/topic/incidents", mapToResponse(incident));
        messagingTemplate.convertAndSend("/topic/alerts", alert);

        return mapToResponse(incident);
    }

    /**
     * Get all active incidents.
     */
    public List<IncidentResponse> getActiveIncidents() {
        return incidentRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Resolve an incident → AUTO REVERT signals.
     */
    @Transactional
    public IncidentResponse resolveIncident(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Incident not found: " + id));

        incident.setActive(false);
        incident.setResolvedAt(LocalDateTime.now());
        incidentRepository.save(incident);

        // ━━━ AUTO REVERT EMERGENCY ━━━
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ INCIDENT #{} RESOLVED — Reverting emergency signals", id);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        emergencyHandler.handleIncidentResolved(id);

        // Generate resolution alert
        Alert resolveAlert = Alert.builder()
                .junctionCode(incident.getJunctionCode())
                .alertType(AlertType.INCIDENT_REPORTED)
                .severity(AlertSeverity.LOW)
                .message("✅ Incident #" + id + " (" +
                        incident.getIncidentType() + ") RESOLVED. " +
                        "All signals returning to normal.")
                .build();
        alertRepository.save(resolveAlert);
        messagingTemplate.convertAndSend("/topic/alerts", resolveAlert);

        return mapToResponse(incident);
    }

    private IncidentResponse mapToResponse(Incident i) {
        return IncidentResponse.builder()
                .id(i.getId())
                .junctionCode(i.getJunctionCode())
                .incidentType(i.getIncidentType())
                .latitude(i.getLatitude())
                .longitude(i.getLongitude())
                .description(i.getDescription())
                .severity(i.getSeverity())
                .active(i.isActive())
                .reportedBy(i.getReportedBy())
                .reportedAt(i.getReportedAt())
                .resolvedAt(i.getResolvedAt())
                .build();
    }
}