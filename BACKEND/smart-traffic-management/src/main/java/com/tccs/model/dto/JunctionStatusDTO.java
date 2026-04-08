package com.tccs.model.dto;

import com.tccs.model.enums.ControlMode;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class JunctionStatusDTO {

    private Long junctionId;
    private String junctionCode;
    private String name;
    private double latitude;
    private double longitude;
    private ControlMode controlMode;
    private boolean active;
    private int totalCycleTime;
    private int currentPhaseIndex;
    private double congestionLevel;    // computed 0.0 - 1.0
    private String congestionLabel;    // LOW / MEDIUM / HIGH / CRITICAL
    private int totalVehicles;
    private double averageSpeed;
    private List<SignalStatusDTO> signals;
}