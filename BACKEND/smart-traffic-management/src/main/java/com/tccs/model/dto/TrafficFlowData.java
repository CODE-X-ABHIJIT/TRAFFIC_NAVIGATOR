package com.tccs.model.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TrafficFlowData {

    private double currentSpeed;
    private double freeFlowSpeed;
    private double currentTravelTime;
    private double freeFlowTravelTime;
    private double confidence;
    private int roadClosure;         // 0 = open, 1 = closed
}