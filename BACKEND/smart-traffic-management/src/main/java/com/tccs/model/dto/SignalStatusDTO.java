package com.tccs.model.dto;

import com.tccs.model.enums.SignalState;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SignalStatusDTO {

    private Long signalId;
    private String direction;
    private SignalState state;
    private int greenDuration;
    private int countdownSeconds;
    private int vehicleDensity;
    private double vehicleSpeed;
    private double freeFlowSpeed;
    private int phaseOrder;
}