package com.tccs.service;

import com.tccs.model.dto.DashboardSummaryDTO;
import com.tccs.model.dto.JunctionStatusDTO;
import com.tccs.model.entity.TrafficLog;
import com.tccs.model.enums.ControlMode;
import com.tccs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JunctionRepository junctionRepository;
    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final TrafficLogRepository trafficLogRepository;
    private final TrafficDataService trafficDataService;

    /**
     * Dashboard summary with all KPIs.
     */
    public DashboardSummaryDTO getDashboardSummary() {
        List<JunctionStatusDTO> junctionStatuses =
                trafficDataService.getAllJunctionStatuses();

        int total = junctionStatuses.size();
        int active = (int) junctionStatuses.stream()
                .filter(JunctionStatusDTO::isActive).count();
        int autoMode = (int) junctionStatuses.stream()
                .filter(j -> j.getControlMode() == ControlMode.AUTO).count();

        double avgCongestion = junctionStatuses.stream()
                .mapToDouble(JunctionStatusDTO::getCongestionLevel)
                .average().orElse(0.0);
        int totalVehicles = junctionStatuses.stream()
                .mapToInt(JunctionStatusDTO::getTotalVehicles).sum();
        double avgSpeed = junctionStatuses.stream()
                .mapToDouble(JunctionStatusDTO::getAverageSpeed)
                .average().orElse(0.0);

        String overallStatus;
        if (avgCongestion >= 0.7) overallStatus = "CRITICAL";
        else if (avgCongestion >= 0.5) overallStatus = "BUSY";
        else if (avgCongestion >= 0.3) overallStatus = "MODERATE";
        else overallStatus = "NORMAL";

        return DashboardSummaryDTO.builder()
                .totalJunctions(total)
                .activeJunctions(active)
                .autoModeCount(autoMode)
                .manualModeCount(total - autoMode)
                .activeIncidents((int) incidentRepository.countByActiveTrue())
                .unacknowledgedAlerts((int) alertRepository.countByAcknowledgedFalse())
                .averageCongestion(Math.round(avgCongestion * 100.0) / 100.0)
                .totalVehiclesDetected(totalVehicles)
                .averageCitySpeed(Math.round(avgSpeed * 10.0) / 10.0)
                .overallStatus(overallStatus)
                .junctions(junctionStatuses)
                .build();
    }

    /**
     * Congestion trends over the last N hours.
     */
    public List<Map<String, Object>> getCongestionTrends(
            String junctionCode, int hours) {
        LocalDateTime from = LocalDateTime.now().minusHours(hours);
        LocalDateTime to = LocalDateTime.now();

        List<TrafficLog> logs;
        if (junctionCode != null && !junctionCode.isEmpty()) {
            logs = trafficLogRepository.findByJunctionCodeAndTimeRange(
                    junctionCode, from, to);
        } else {
            logs = trafficLogRepository.findAllByTimeRange(from, to);
        }

        return logs.stream()
                .map(log -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("junctionCode", log.getJunctionCode());
                    point.put("timestamp", log.getRecordedAt().toString());
                    point.put("congestionLevel", log.getCongestionLevel());
                    point.put("speed", log.getCurrentSpeed());
                    point.put("density", log.getTotalVehicleDensity());
                    point.put("waitTime", log.getAverageWaitTime());
                    return point;
                })
                .collect(Collectors.toList());
    }

    /**
     * Peak hours analysis based on historical data.
     */
    public Map<Integer, Double> getPeakHourAnalysis(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<TrafficLog> logs = trafficLogRepository.findAllByTimeRange(
                from, LocalDateTime.now());

        // Group by hour and average congestion
        return logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getRecordedAt().getHour(),
                        TreeMap::new,
                        Collectors.averagingDouble(TrafficLog::getCongestionLevel)
                ));
    }
}