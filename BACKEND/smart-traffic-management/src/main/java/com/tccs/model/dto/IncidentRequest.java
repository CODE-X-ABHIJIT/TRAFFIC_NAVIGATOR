package com.tccs.model.dto;

import com.tccs.model.enums.AlertSeverity;
import com.tccs.model.enums.IncidentType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IncidentRequest {

    private String junctionCode;

    @NotNull(message = "Incident type is required")
    private IncidentType incidentType;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String description;
    private AlertSeverity severity;
    private String reportedBy;
}