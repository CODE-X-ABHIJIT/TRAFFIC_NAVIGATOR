package com.tccs.model.dto;

import com.tccs.model.enums.AlertSeverity;
import com.tccs.model.enums.IncidentType;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IncidentResponse {

    private Long id;
    private String junctionCode;
    private IncidentType incidentType;
    private double latitude;
    private double longitude;
    private String description;
    private AlertSeverity severity;
    private boolean active;
    private String reportedBy;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
}