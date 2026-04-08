package com.tccs.service;

import com.tccs.engine.CongestionDetector;
import com.tccs.integration.TomTomTrafficClient;
import com.tccs.model.dto.*;
import com.tccs.model.entity.*;
import com.tccs.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficDataService {

    private final JunctionRepository junctionRepository;
    private final SignalRepository signalRepository;
    private final TrafficLogRepository trafficLogRepository;
    private final TomTomTrafficClient tomTomClient;
    private final CongestionDetector congestionDetector;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Fetches live traffic data for all junctions and updates densities/speeds.
     * Called by TrafficDataScheduler every 30 seconds.
     */
    @Transactional
    public void refreshAllTrafficData() {
        List<Junction> junctions = junctionRepository.findByActiveTrue();
        Random rand = new Random();

        for (Junction junction : junctions) {
            for (Signal signal : junction.getSignals()) {
                TrafficFlowData flowData = tomTomClient.getTrafficFlow(
                        junction.getLatitude(), junction.getLongitude());

                // Add per-direction variation
                double speedVariation = 0.8 + rand.nextDouble() * 0.4;
                double speed = flowData.getCurrentSpeed() * speedVariation;

                signal.setVehicleSpeed(Math.round(speed * 10.0) / 10.0);
                signal.setFreeFlowSpeed(flowData.getFreeFlowSpeed());

                // Simulate vehicle density based on speed ratio
                double congestion = congestionDetector.calculateCongestion(
                        speed, flowData.getFreeFlowSpeed());
                int density = (int) (congestion * 60 + rand.nextInt(15));
                signal.setVehicleDensity(Math.max(0, density));
            }

            signalRepository.saveAll(junction.getSignals());

            // Log traffic data for analytics
            logTrafficData(junction);
        }

        // Broadcast updated statuses via WebSocket
        broadcastAllStatuses();
        log.debug("Traffic data refreshed for {} junctions", junctions.size());
    }

    /**
     * Gets live status for a specific junction.
     */
    public JunctionStatusDTO getJunctionStatus(String junctionCode) {
        Junction junction = junctionRepository.findByJunctionCode(junctionCode)
                .orElseThrow(() -> new com.tccs.exception.ResourceNotFoundException(
                        "Junction not found: " + junctionCode));
        return mapToDTO(junction);
    }

    /**
     * Gets live status for ALL junctions.
     */
    public List<JunctionStatusDTO> getAllJunctionStatuses() {
        return junctionRepository.findByActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Generates congestion heatmap data.
     */
    public List<CongestionHeatmapPoint> getHeatmapData() {
        return junctionRepository.findByActiveTrue().stream()
                .map(j -> {
                    double avgSpeed = j.getSignals().stream()
                            .mapToDouble(Signal::getVehicleSpeed)
                            .average().orElse(0);
                    double avgFreeFlow = j.getSignals().stream()
                            .mapToDouble(Signal::getFreeFlowSpeed)
                            .average().orElse(60);
                    double congestion = congestionDetector.calculateCongestion(
                            avgSpeed, avgFreeFlow);
                    int totalDensity = j.getSignals().stream()
                            .mapToInt(Signal::getVehicleDensity).sum();

                    return CongestionHeatmapPoint.builder()
                            .junctionCode(j.getJunctionCode())
                            .junctionName(j.getName())
                            .latitude(j.getLatitude())
                            .longitude(j.getLongitude())
                            .congestionLevel(congestion)
                            .intensity(congestion)
                            .vehicleDensity(totalDensity)
                            .avgSpeed(Math.round(avgSpeed * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Private Helpers ───────────────────────────────

    private JunctionStatusDTO mapToDTO(Junction junction) {
        List<Signal> signals = junction.getSignals();

        double avgSpeed = signals.stream()
                .mapToDouble(Signal::getVehicleSpeed).average().orElse(0);
        double avgFreeFlow = signals.stream()
                .mapToDouble(Signal::getFreeFlowSpeed).average().orElse(60);
        double congestion = congestionDetector.calculateCongestion(avgSpeed, avgFreeFlow);
        int totalVehicles = signals.stream()
                .mapToInt(Signal::getVehicleDensity).sum();

        List<SignalStatusDTO> signalDTOs = signals.stream()
                .map(s -> SignalStatusDTO.builder()
                        .signalId(s.getId())
                        .direction(s.getDirection())
                        .state(s.getCurrentState())
                        .greenDuration(s.getGreenDuration())
                        .countdownSeconds(s.getCountdownSeconds())
                        .vehicleDensity(s.getVehicleDensity())
                        .vehicleSpeed(s.getVehicleSpeed())
                        .freeFlowSpeed(s.getFreeFlowSpeed())
                        .phaseOrder(s.getPhaseOrder())
                        .build())
                .sorted(Comparator.comparingInt(SignalStatusDTO::getPhaseOrder))
                .collect(Collectors.toList());

        return JunctionStatusDTO.builder()
                .junctionId(junction.getId())
                .junctionCode(junction.getJunctionCode())
                .name(junction.getName())
                .latitude(junction.getLatitude())
                .longitude(junction.getLongitude())
                .controlMode(junction.getControlMode())
                .active(junction.isActive())
                .totalCycleTime(junction.getTotalCycleTime())
                .currentPhaseIndex(junction.getCurrentPhaseIndex())
                .congestionLevel(Math.round(congestion * 100.0) / 100.0)
                .congestionLabel(congestionDetector.getCongestionLabel(congestion))
                .totalVehicles(totalVehicles)
                .averageSpeed(Math.round(avgSpeed * 10.0) / 10.0)
                .signals(signalDTOs)
                .build();
    }

    private void logTrafficData(Junction junction) {
        List<Signal> signals = junction.getSignals();
        double avgSpeed = signals.stream()
                .mapToDouble(Signal::getVehicleSpeed).average().orElse(0);
        double avgFreeFlow = signals.stream()
                .mapToDouble(Signal::getFreeFlowSpeed).average().orElse(60);
        double congestion = congestionDetector.calculateCongestion(avgSpeed, avgFreeFlow);
        int totalDensity = signals.stream()
                .mapToInt(Signal::getVehicleDensity).sum();

        TrafficLog logEntry = TrafficLog.builder()
                .junctionCode(junction.getJunctionCode())
                .currentSpeed(avgSpeed)
                .freeFlowSpeed(avgFreeFlow)
                .congestionLevel(congestion)
                .totalVehicleDensity(totalDensity)
                .averageWaitTime(congestion * 90) // estimated wait
                .build();

        trafficLogRepository.save(logEntry);
    }

    private void broadcastAllStatuses() {
        List<JunctionStatusDTO> statuses = getAllJunctionStatuses();
        messagingTemplate.convertAndSend("/topic/junction-updates", statuses);
    }
}