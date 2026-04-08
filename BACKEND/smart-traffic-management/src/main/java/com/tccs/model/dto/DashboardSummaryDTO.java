package com.tccs.model.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DashboardSummaryDTO {

    private int totalJunctions;
    private int activeJunctions;
    private int autoModeCount;
    private int manualModeCount;
    private int activeIncidents;
    private int unacknowledgedAlerts;
    private double averageCongestion;       // city-wide average
    private int totalVehiclesDetected;
    private double averageCitySpeed;
    private String overallStatus;           // NORMAL / BUSY / CRITICAL
    private List<JunctionStatusDTO> junctions;
}