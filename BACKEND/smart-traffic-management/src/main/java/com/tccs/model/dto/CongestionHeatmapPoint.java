package com.tccs.model.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CongestionHeatmapPoint {

    private String junctionCode;
    private String junctionName;
    private double latitude;
    private double longitude;
    private double congestionLevel;  // 0.0 - 1.0
    private double intensity;        // for heatmap rendering
    private int vehicleDensity;
    private double avgSpeed;
}