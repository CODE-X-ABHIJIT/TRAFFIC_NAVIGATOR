package com.tccs.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TrafficLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "junction_code", nullable = false)
    private String junctionCode;

    @Column(name = "current_speed")
    private double currentSpeed;

    @Column(name = "free_flow_speed")
    private double freeFlowSpeed;

    @Column(name = "congestion_level")
    private double congestionLevel; // 0.0 to 1.0

    @Column(name = "total_vehicle_density")
    private int totalVehicleDensity;

    @Column(name = "average_wait_time")
    private double averageWaitTime; // seconds

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}