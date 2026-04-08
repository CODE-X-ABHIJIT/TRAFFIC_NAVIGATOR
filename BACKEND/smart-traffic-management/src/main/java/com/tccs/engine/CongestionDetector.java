package com.tccs.engine;

import com.tccs.model.entity.Alert;
import com.tccs.model.entity.Signal;
import com.tccs.model.enums.AlertSeverity;
import com.tccs.model.enums.AlertType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects congestion conditions and generates alerts.
 */
@Component
@Slf4j
public class CongestionDetector {

    /**
     * Calculates congestion level (0.0 = free flow, 1.0 = standstill)
     */
    public double calculateCongestion(double currentSpeed, double freeFlowSpeed) {
        if (freeFlowSpeed <= 0) return 0.0;
        double ratio = currentSpeed / freeFlowSpeed;
        return Math.max(0.0, Math.min(1.0, 1.0 - ratio));
    }

    /**
     * Returns human-readable congestion label
     */
    public String getCongestionLabel(double congestionLevel) {
        if (congestionLevel >= 0.75) return "CRITICAL";
        if (congestionLevel >= 0.50) return "HIGH";
        if (congestionLevel >= 0.25) return "MEDIUM";
        return "LOW";
    }

    /**
     * Returns the AlertSeverity for given congestion
     */
    public AlertSeverity getSeverity(double congestionLevel) {
        if (congestionLevel >= 0.75) return AlertSeverity.CRITICAL;
        if (congestionLevel >= 0.50) return AlertSeverity.HIGH;
        if (congestionLevel >= 0.25) return AlertSeverity.MEDIUM;
        return AlertSeverity.LOW;
    }

    /**
     * Analyzes signals at a junction and produces alerts if needed.
     */
    public List<Alert> analyzeJunction(String junctionCode, List<Signal> signals) {
        List<Alert> alerts = new ArrayList<>();

        for (Signal signal : signals) {
            double congestion = calculateCongestion(
                    signal.getVehicleSpeed(), signal.getFreeFlowSpeed());

            // High congestion alert
            if (congestion >= 0.50) {
                alerts.add(Alert.builder()
                        .junctionCode(junctionCode)
                        .alertType(AlertType.CONGESTION)
                        .severity(getSeverity(congestion))
                        .message(String.format(
                                "High congestion at %s - %s lane. Speed: %.0f km/h " +
                                "(Free flow: %.0f km/h). Congestion: %.0f%%",
                                junctionCode, signal.getDirection(),
                                signal.getVehicleSpeed(), signal.getFreeFlowSpeed(),
                                congestion * 100))
                        .build());
            }

            // Speed drop alert
            if (signal.getFreeFlowSpeed() > 0 &&
                signal.getVehicleSpeed() < signal.getFreeFlowSpeed() * 0.3) {
                alerts.add(Alert.builder()
                        .junctionCode(junctionCode)
                        .alertType(AlertType.SPEED_DROP)
                        .severity(AlertSeverity.HIGH)
                        .message(String.format(
                                "Severe speed drop at %s - %s lane. " +
                                "Current: %.0f km/h, Expected: %.0f km/h",
                                junctionCode, signal.getDirection(),
                                signal.getVehicleSpeed(), signal.getFreeFlowSpeed()))
                        .build());
            }

            // Density spike alert
            if (signal.getVehicleDensity() > 50) {
                alerts.add(Alert.builder()
                        .junctionCode(junctionCode)
                        .alertType(AlertType.DENSITY_SPIKE)
                        .severity(AlertSeverity.MEDIUM)
                        .message(String.format(
                                "High vehicle density at %s - %s lane: %d vehicles",
                                junctionCode, signal.getDirection(),
                                signal.getVehicleDensity()))
                        .build());
            }
        }

        return alerts;
    }
}